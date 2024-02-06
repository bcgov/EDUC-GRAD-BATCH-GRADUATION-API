package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.*;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.processor.*;
import ca.bc.gov.educ.api.batchgraduation.reader.*;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.writer.*;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.UUID;

@Configuration
@EnableBatchProcessing(isolationLevelForCreate = "ISOLATION_READ_COMMITTED")
public class BatchJobConfig {

    // Partitioning for Regular Grad Run updates

//    @Autowired
//    JobRegistry jobRegistry;

    /**
     * Regular Grad Algorithm Run
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    @StepScope
    public ItemProcessor<UUID,GraduationStudentRecord> itemProcessorRegGrad() {
        return new RunRegularGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderRegGrad() {
        return new RecalculateStudentReader();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderRegErrorGrad() {
        return new RecalculateStudentErrorReader();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderRegErrorRetryGrad() {
        return new RecalculateStudentErrorRetryReader();
    }


    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterRegGrad() {
        return new RegGradAlgBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepRegGrad(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepRegGrad", jobRepository)
                .partitioner(graduationJobStep(jobRepository, transactionManager, skipListener, constants).getName(), partitionerRegGrad())
                .step(graduationJobStep(jobRepository, transactionManager, skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepRegGradError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepRegGradError", jobRepository)
                .partitioner(graduationJobErrorStep(jobRepository, transactionManager, skipListener, constants).getName(), partitionerRegGradRetry())
                .step(graduationJobErrorStep(jobRepository, transactionManager, skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepRegGradErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepRegGradErrorRetry", jobRepository)
                .partitioner(graduationJobErrorRetryStep(jobRepository, transactionManager, skipListener, constants).getName(), partitionerRegGradRetry())
                .step(graduationJobErrorRetryStep(jobRepository, transactionManager, skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }


    @Bean
    @StepScope
    public RegGradAlgPartitioner partitionerRegGrad() {
        return new RegGradAlgPartitioner();
    }

    @Bean
    @StepScope
    public RegGradAlgPartitionerRetry partitionerRegGradRetry() {
        return new RegGradAlgPartitionerRetry();
    }

    @Bean
    public Step graduationJobErrorStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("graduationJobErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderRegErrorGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step graduationJobErrorRetryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("graduationJobErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderRegErrorRetryGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step graduationJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("graduationJobStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderRegGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean(name="GraduationBatchJob")
    public Job graduationBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, GradRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("GraduationBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepRegGrad(jobRepository, transactionManager, constants, skipListener))
                .on("*")
                .to(masterStepRegGradError(jobRepository, transactionManager,constants, skipListener))
                .on("*")
                .to(masterStepRegGradErrorRetry(jobRepository, transactionManager,constants, skipListener))
                .on("*")
                .end().build()
                .build();
    }


    /**
     * TVR Projected Grad Run
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    @StepScope
    public ItemProcessor<UUID,GraduationStudentRecord> itemProcessorTvrRun() {
        return new RunProjectedGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderTvrRun() {
        return new RecalculateProjectedGradRunReader();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderTvrErrorRun() {
        return new RecalculateProjectedGradRunErrorReader();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderTvrErrorRetryRun() {
        return new RecalculateProjectedGradRunErrorRetryReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterTvrRun() {
        return new TvrRunBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepTvrRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepTvrRun", jobRepository)
                .partitioner(tvrJobStep(jobRepository, transactionManager,skipListener, constants).getName(), partitionerTvrRun())
                .step(tvrJobStep(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepTvrRunError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepTvrRunError", jobRepository)
                .partitioner(tvrJobErrorStep(jobRepository, transactionManager,skipListener, constants).getName(), partitionerTvrRunRetry())
                .step(tvrJobErrorStep(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepTvrRunErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepTvrRunErrorRetry", jobRepository)
                .partitioner(tvrJobErrorRetryStep(jobRepository, transactionManager,skipListener, constants).getName(), partitionerTvrRunRetry())
                .step(tvrJobErrorRetryStep(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    @StepScope
    public TvrRunPartitioner partitionerTvrRun() {
        return new TvrRunPartitioner();
    }

    @Bean
    @StepScope
    public TvrRunPartitionerRetry partitionerTvrRunRetry() {
        return new TvrRunPartitionerRetry();
    }


    @Bean
    public Step tvrJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("tvrJobStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderTvrRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step tvrJobErrorStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("tvrJobErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderTvrErrorRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step tvrJobErrorRetryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("tvrJobErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderTvrErrorRetryRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean(name="tvrBatchJob")
    public Job tvrBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, TvrRunJobCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("tvrBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepTvrRun(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .to(masterStepTvrRunError(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .to(masterStepTvrRunErrorRetry(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .end().build()
                .build();
    }

    /**
     * Special Regular Grad Algorithm Run
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    @StepScope
    public ItemProcessor<UUID,GraduationStudentRecord> itemProcessorSpcRegGrad() {
        return new RunSpecialGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderSpcRegGrad() {
        return new SpecialGradRunStudentReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterSpcRegGrad() {
        return new RegGradAlgBatchPerformanceWriter();
    }



    @Bean
    public Step masterStepSpcRegGrad(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcRegGrad", jobRepository)
                .partitioner(slaveSpcRegGradStep(jobRepository, transactionManager,skipListener, constants).getName(), partitionerSpcRegGrad())
                .step(slaveSpcRegGradStep(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepSpcRegGradError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcRegGradError", jobRepository)
                .partitioner(slaveSpcRegGradErrorStep(jobRepository, transactionManager,skipListener, constants).getName(), partitionerSpcRegGradRetry())
                .step(slaveSpcRegGradErrorStep(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepSpcRegGradErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcRegGradErrorRetry", jobRepository)
                .partitioner(slaveSpcRegGradErrorRetryStep(jobRepository, transactionManager,skipListener, constants).getName(), partitionerSpcRegGradRetry())
                .step(slaveSpcRegGradErrorRetryStep(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    @StepScope
    public SpcRegGradAlgPartitioner partitionerSpcRegGrad() {
        return new SpcRegGradAlgPartitioner();
    }

    @Bean
    @StepScope
    public SpcRegGradAlgPartitionerRetry partitionerSpcRegGradRetry() {
        return new SpcRegGradAlgPartitionerRetry();
    }

    @Bean
    public Step slaveSpcRegGradStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveStepSpcRegGrad", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step slaveSpcRegGradErrorStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveSpcRegGradErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step slaveSpcRegGradErrorRetryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveSpcRegGradErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean(name="SpecialGraduationBatchJob")
    public Job specialGraduationBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, SpecialRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("SpecialGraduationBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepSpcRegGrad(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .to(masterStepSpcRegGradError(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .to(masterStepSpcRegGradErrorRetry(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .end().build()
                .build();
    }

    /**
     * Special TVR Projected Grad Run
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    @StepScope
    public ItemProcessor<UUID,GraduationStudentRecord> itemProcessorSpcTvrRun() {
        return new RunSpecialProjectedGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderSpcTvrRun() {
        return new SpecialProjectedGradRunReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterSpcTvrRun() {
        return new TvrRunBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepSpcTvrRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcTvrRun", jobRepository)
                .partitioner(slaveStepSpcTvrRun(jobRepository, transactionManager,skipListener, constants).getName(), partitionerSpcRegGrad())
                .step(slaveStepSpcTvrRun(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepSpcTvrRunError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcTvrRunError", jobRepository)
                .partitioner(slaveStepSpcTvrRunError(jobRepository, transactionManager,skipListener, constants).getName(), partitionerSpcRegGradRetry())
                .step(slaveStepSpcTvrRunError(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepSpcTvrRunErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcTvrRunErrorRetry", jobRepository)
                .partitioner(slaveStepSpcTvrRunErrorRetry(jobRepository, transactionManager,skipListener, constants).getName(), partitionerSpcRegGradRetry())
                .step(slaveStepSpcTvrRunErrorRetry(jobRepository, transactionManager,skipListener, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveStepSpcTvrRun", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRunError(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveStepSpcTvrRunError", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRunErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveStepSpcTvrRunErrorRetry", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean(name="SpecialTvrRunBatchJob")
    public Job specialTvrRunBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, SpecialRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("SpecialTvrRunBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepSpcTvrRun(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .to(masterStepSpcTvrRunError(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .to(masterStepSpcTvrRunErrorRetry(jobRepository, transactionManager,constants,skipListener))
                .on("*")
                .end().build()
                .build();
    }

    /**
     * Monthly Distribution Run
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    @StepScope
    public ItemProcessor<StudentCredentialDistribution,StudentCredentialDistribution> itemProcessorDisRun() {
        return new DistributionRunProcessor();
    }

    @Bean
    @StepScope
    public ItemProcessor<String, List<StudentCredentialDistribution>> itemProcessorDisRunYearlyNonGradByMincode() {
        return new DistributionRunYearlyNonGradProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<StudentCredentialDistribution> itemReaderDisRun() {
        return new DistributionRunStudentCredentialsReader();
    }

    @Bean
    @StepScope
    public ItemReader<String> itemReaderDisRunYearlyNonGrad() {
        return new DistributionRunYearlyNonGradReader();
    }

    @Bean
    @StepScope
    public ItemWriter<StudentCredentialDistribution> itemWriterDisRun() {
        return new DistributionRunWriter();
    }

    @Bean
    @StepScope
    public ItemWriter<List<StudentCredentialDistribution>> itemWriterDisRunYearlyNonGrad() {
        return new DistributionRunYearlyNonGradWriter();
    }

    @Bean
    @StepScope
    public DistributionRunPartitioner partitionerDisRun() {
        return new DistributionRunPartitioner();
    }


    @Bean
    public Step slaveStepDisRun(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("slaveStepDisRun", jobRepository)
                .<StudentCredentialDistribution, StudentCredentialDistribution>chunk(1, transactionManager)
                .reader(itemReaderDisRun())
                .processor(itemProcessorDisRun())
                .writer(itemWriterDisRun())
                .build();
    }

    @Bean
    public Step slaveStepDisRunYearly(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("slaveStepDisRun", jobRepository)
                .<StudentCredentialDistribution, StudentCredentialDistribution>chunk(1, transactionManager)
                .reader(itemReaderDisRun())
                .processor(itemProcessorDisRun())
                .writer(itemWriterDisRun())
                .build();
    }

    @Bean
    public Step slaveStepDisRunYearlyNonGradByMincode(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("slaveStepDisRunYearlyNonGrad", jobRepository)
                .<String, List<StudentCredentialDistribution>>chunk(1, transactionManager)
                .reader(itemReaderDisRunYearlyNonGrad())
                .processor(itemProcessorDisRunYearlyNonGradByMincode())
                .writer(itemWriterDisRunYearlyNonGrad())
                .build();
    }

    @Bean
    public Step masterStepDisRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDisRun", jobRepository)
                .partitioner(slaveStepDisRun(jobRepository, transactionManager).getName(), partitionerDisRun())
                .step(slaveStepDisRun(jobRepository, transactionManager))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean(name="DistributionBatchJob")
    public Job distributionBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, DistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("DistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRun(jobRepository,transactionManager, constants))
                .end()
                .build();
    }

    /**
     * Yearly Distribution Run
     * ItemProcessor,ItemReader and ItemWriter common with monthly distribution run
     * Partitioner separate
     */

    @Bean
    @StepScope
    public DistributionRunYearlyPartitioner partitionerDisRunYearly() {
        return new DistributionRunYearlyPartitioner();
    }

    @Bean
    public Step masterStepDisRunYearly(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDisRunYearly", jobRepository)
                .partitioner(slaveStepDisRun(jobRepository, transactionManager).getName(), partitionerDisRunYearly())
                .step(slaveStepDisRunYearly(jobRepository, transactionManager))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean(name="YearlyDistributionBatchJob")
    public Job distributionBatchJobYearly(JobRepository jobRepository, PlatformTransactionManager transactionManager, DistributionRunYearlyCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("YearlyDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRunYearly(jobRepository, transactionManager,constants))
                .end()
                .build();
    }

    @Bean
    @StepScope
    public DistributionRunYearlyNonGradPartitioner partitionerDisRunYearlyNonGrad() {
        return new DistributionRunYearlyNonGradPartitioner();
    }

    @Bean
    public Step masterStepDisRunYearlyNonGrad(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDisRunYearlyNonGrad", jobRepository)
                .partitioner(slaveStepDisRunYearlyNonGradByMincode(jobRepository, transactionManager).getName(), partitionerDisRunYearlyNonGrad())
                .step(slaveStepDisRunYearlyNonGradByMincode(jobRepository, transactionManager))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean(name="YearlyNonGradDistributionBatchJob")
    public Job distributionBatchJobYearlyNonGrad(JobRepository jobRepository, PlatformTransactionManager transactionManager, DistributionRunYearlyNonGradCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("YearlyNonGradDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRunYearlyNonGrad(jobRepository, transactionManager,constants))
                .end()
                .build();
    }

    @Bean
    @StepScope
    public DistributionRunSupplementalPartitioner partitionerDisRunSupplemental() {
        return new DistributionRunSupplementalPartitioner();
    }

    @Bean
    public Step masterStepDisRunSupplemental(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDisRunSupplemental", jobRepository)
                .partitioner(slaveStepDisRun(jobRepository, transactionManager).getName(), partitionerDisRunSupplemental())
                .step(slaveStepDisRun(jobRepository, transactionManager))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean(name="SupplementalDistributionBatchJob")
    public Job distributionBatchJobSupplemental(JobRepository jobRepository, PlatformTransactionManager transactionManager, DistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("SupplementalDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRunSupplemental(jobRepository, transactionManager,constants))
                .end()
                .build();
    }

    /**
     * User Distribution Run
     * ItemProcessor,ItemReader and ItemWriter common with monthly distribution run
     * Partitioner separate
     */

    @Bean
    @StepScope
    public DistributionRunPartitionerUserReq partitionerDisRunUserReq() {
        return new DistributionRunPartitionerUserReq();
    }

    @Bean
    public Step masterStepUserReqDisRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepUserReqDisRun", jobRepository)
                .partitioner(slaveStepDisRun(jobRepository, transactionManager).getName(), partitionerDisRunUserReq())
                .step(slaveStepDisRun(jobRepository, transactionManager))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean(name="UserReqDistributionBatchJob")
    public Job distributionBatchJobUserReq(JobRepository jobRepository, PlatformTransactionManager transactionManager, UserReqDistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("UserReqDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepUserReqDisRun(jobRepository, transactionManager,constants))
                .end()
                .build();
    }

    /**
     * User Blank Credential Distribution Run
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    @StepScope
    public ItemProcessor<BlankCredentialDistribution,BlankCredentialDistribution> itemProcessorBlankDisRun() {
        return new BlankDistributionRunProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<BlankCredentialDistribution> itemReaderBlankDisRun() {
        return new BlankDistributionRunReader();
    }

    @Bean
    @StepScope
    public ItemWriter<BlankCredentialDistribution> itemWriterBlankDisRun() {
        return new BlankDistributionRunWriter();
    }


    @Bean
    @StepScope
    public DistributionRunPartitionerBlankUserReq partitionerDisRunBlankUserReq() {
        return new DistributionRunPartitionerBlankUserReq();
    }

    @Bean
    public Step slaveStepBlankDisRun(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("slaveStepBlankDisRun", jobRepository)
                .<BlankCredentialDistribution, BlankCredentialDistribution>chunk(1, transactionManager)
                .reader(itemReaderBlankDisRun())
                .processor(itemProcessorBlankDisRun())
                .writer(itemWriterBlankDisRun())
                .build();
    }

    @Bean
    public Step masterStepBlankUserReqDisRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepBlankUserReqDisRun", jobRepository)
                .partitioner(slaveStepBlankDisRun(jobRepository, transactionManager).getName(), partitionerDisRunBlankUserReq())
                .step(slaveStepBlankDisRun(jobRepository, transactionManager))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }


    @Bean(name="blankDistributionBatchJob")
    public Job blankDistributionBatchJobUserReq(JobRepository jobRepository, PlatformTransactionManager transactionManager, UserReqBlankDistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("blankDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepBlankUserReqDisRun(jobRepository, transactionManager,constants))
                .end()
                .build();
    }

    /**
     * User PSI Credential Distribution Run
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    @StepScope
    public ItemProcessor<PsiCredentialDistribution,PsiCredentialDistribution> itemProcessorPsiDisRun() {
        return new PsiDistributionRunProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<PsiCredentialDistribution> itemReaderPsiDisRun() {
        return new PsiDistributionRunReader();
    }

    @Bean
    @StepScope
    public ItemWriter<PsiCredentialDistribution> itemWriterPsiDisRun() {
        return new PsiDistributionRunWriter();
    }


    @Bean
    @StepScope
    public DistributionRunPartitionerPsiUserReq partitionerDisRunPsiUserReq() {
        return new DistributionRunPartitionerPsiUserReq();
    }

    @Bean
    public Step slaveStepPsiDisRun(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("slaveStepPsiDisRun", jobRepository)
                .<PsiCredentialDistribution, PsiCredentialDistribution>chunk(1, transactionManager)
                .reader(itemReaderPsiDisRun())
                .processor(itemProcessorPsiDisRun())
                .writer(itemWriterPsiDisRun())
                .build();
    }

    @Bean
    public Step masterStepPsiUserReqDisRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepPsiUserReqDisRun", jobRepository)
                .partitioner(slaveStepPsiDisRun(jobRepository, transactionManager).getName(), partitionerDisRunPsiUserReq())
                .step(slaveStepPsiDisRun(jobRepository, transactionManager))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }


    @Bean(name="psiDistributionBatchJob")
    public Job psiDistributionBatchJobUserReq(JobRepository jobRepository, PlatformTransactionManager transactionManager, UserReqPsiDistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("psiDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepPsiUserReqDisRun(jobRepository, transactionManager,constants))
                .end()
                .build();
    }

    /**
     * Regenerate Current Student's Certificates whose distribution date is null
     */
    @Bean
    @StepScope
    public RegenerateCertificatePartitioner partitionerCertRegen() {
        return new RegenerateCertificatePartitioner();
    }

    @Bean
    @StepScope
    public ItemReader<UUID> itemReaderCertRegen() {
        return new RegenerateCertificateReader();
    }

    @Bean
    @StepScope
    public ItemWriter<Integer> itemWriterCertRegen() {
        return new RegenerateCertificateRunWriter();
    }

    @Bean
    @StepScope
    public ItemProcessor<UUID,Integer> itemProcessorCertRegen() {
        return new RunCertificateRegenerationProcessor();
    }

    @Bean
    public Step masterStepCertRegen(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepCertRegen", jobRepository)
                .partitioner(certRegenJobStep(jobRepository, transactionManager, skipListener).getName(), partitionerCertRegen())
                .step(certRegenJobStep(jobRepository, transactionManager, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step certRegenJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("certRegenJobStep", jobRepository)
                .<UUID, Integer>chunk(1, transactionManager)
                .reader(itemReaderCertRegen())
                .processor(itemProcessorCertRegen())
                .writer(itemWriterCertRegen())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean(name="certRegenBatchJob")
    public Job certRegenBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, RegenCertRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("certRegenBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepCertRegen(jobRepository, transactionManager,constants, skipListener))
                .on("*")
                .end().build()
                .build();
    }

    /**
     * Create Graduation Status snapshot for EDW
     */
    @Bean
    @StepScope
    public EDWSnapshotSchoolPartitioner partitionerEDWSnapshotSchool() {
        return new EDWSnapshotSchoolPartitioner();
    }

    @Bean
    @StepScope
    public ItemReader<String> itemReaderEDWSnapshotSchool() {
        return new EDWSnapshotSchoolReader();
    }

    @Bean
    @StepScope
    public ItemWriter<List<Pair<String, List<SnapshotResponse>>>> itemWriterEDWSnapshotSchool() {
        return new EDWSnapshotSchoolWriter();
    }

    @Bean
    @StepScope
    public ItemProcessor<String,List<Pair<String, List<SnapshotResponse>>>> itemProcessorEDWSnapshotSchool() {
        return new EDWSnapshotSchoolProcessor();
    }

    @Bean
    public Step masterStepEdwSnapshotSchool(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepEdwSnapshotSchool", jobRepository)
                .partitioner(edwSnapshotSchoolJobStep(jobRepository, transactionManager, skipListener).getName(), partitionerEDWSnapshotSchool())
                .step(edwSnapshotSchoolJobStep(jobRepository, transactionManager, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step edwSnapshotSchoolJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("edwSnapshotSchoolJobStep", jobRepository)
                .<String, List<Pair<String, List<SnapshotResponse>>>>chunk(1, transactionManager)
                .reader(itemReaderEDWSnapshotSchool())
                .processor(itemProcessorEDWSnapshotSchool())
                .writer(itemWriterEDWSnapshotSchool())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    @StepScope
    public EDWSnapshotPartitioner partitionerEDWSnapshot() {
        return new EDWSnapshotPartitioner();
    }

    @Bean
    @StepScope
    public ItemReader<SnapshotResponse> itemReaderEDWSnapshot() {
        return new EDWSnapshotReader();
    }

    @Bean
    @StepScope
    public ItemWriter<EdwGraduationSnapshot> itemWriterEDWSnapshot() {
        return new EDWSnapshotWriter();
    }

    @Bean
    @StepScope
    public ItemProcessor<SnapshotResponse,EdwGraduationSnapshot> itemProcessorEDWSnapshot() {
        return new EDWSnapshotProcessor();
    }

    @Bean
    public Step masterStepEdwSnapshot(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepEdwSnapshot", jobRepository)
                .partitioner(edwSnapshotJobStep(jobRepository, transactionManager, skipListener).getName(), partitionerEDWSnapshot())
                .step(edwSnapshotJobStep(jobRepository, transactionManager, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step edwSnapshotJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("edwSnapshotJobStep", jobRepository)
                .<SnapshotResponse, EdwGraduationSnapshot>chunk(1, transactionManager)
                .reader(itemReaderEDWSnapshot())
                .processor(itemProcessorEDWSnapshot())
                .writer(itemWriterEDWSnapshot())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean(name="edwSnapshotBatchJob")
    public Job edwSnapshotBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, EdwSnapshotCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("edwSnapshotBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepEdwSnapshotSchool(jobRepository, transactionManager,constants, skipListener))
                .on("*")
                .to(masterStepEdwSnapshot(jobRepository, transactionManager,constants, skipListener))
                .on("*")
                .end().build()
                .build();
    }

    /**
     * User Scheduled Jobs Refreshing Map on startup
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    public ItemProcessor<UserScheduledJobs,UserScheduledJobs> itemProcessorUserScheduled() {
        return new UserScheduledProcessor();
    }

    @Bean
    public ItemReader<UserScheduledJobs> itemReaderUserScheduled() {
        return new UserScheduledReader();
    }

    @Bean
    public ItemWriter<UserScheduledJobs> itemWriterUserScheduled() {
        return new UserScheduledWriter();
    }

    @Bean
    public Step slaveStepUserScheduled(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("slaveStepUserScheduled", jobRepository)
                .<UserScheduledJobs, UserScheduledJobs>chunk(1, transactionManager)
                .reader(itemReaderUserScheduled())
                .processor(itemProcessorUserScheduled())
                .writer(itemWriterUserScheduled())
                .build();
    }

    @Bean(name="userScheduledBatchJobRefresher")
    public Job userScheduledBatchJobQueueRefresher(JobRepository jobRepository, PlatformTransactionManager transactionManager, UserScheduledCompletionNotificationListener listener) {
        return new JobBuilder("userScheduledBatchJobRefresher", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(slaveStepUserScheduled(jobRepository, transactionManager))
                .end()
                .build();
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor("partition-");
    }
}