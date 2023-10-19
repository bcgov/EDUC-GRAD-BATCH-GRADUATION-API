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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    // Partitioning for Regular Grad Run updates

    @Autowired
    JobRegistry jobRegistry;

    @Autowired
    private PlatformTransactionManager batchTransactionManager;

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
    public Step masterStepRegGrad(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepRegGrad", jobRepository)
                .partitioner(graduationJobStep(jobRepository, skipListener).getName(), partitionerRegGrad())
                .step(graduationJobStep(jobRepository, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepRegGradError(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepRegGradError", jobRepository)
                .partitioner(graduationJobErrorStep(jobRepository, skipListener).getName(), partitionerRegGradRetry())
                .step(graduationJobErrorStep(jobRepository, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepRegGradErrorRetry(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepRegGradErrorRetry", jobRepository)
                .partitioner(graduationJobErrorRetryStep(jobRepository, skipListener).getName(), partitionerRegGradRetry())
                .step(graduationJobErrorRetryStep(jobRepository, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
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
    public Step graduationJobErrorStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("graduationJobErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderRegErrorGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step graduationJobErrorRetryStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("graduationJobErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderRegErrorRetryGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step graduationJobStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("graduationJobStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderRegGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean(name="GraduationBatchJob")
    public Job graduationBatchJob(JobRepository jobRepository, GradRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("GraduationBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepRegGrad(jobRepository, constants, skipListener))
                .on("*")
                .to(masterStepRegGradError(jobRepository,constants, skipListener))
                .on("*")
                .to(masterStepRegGradErrorRetry(jobRepository,constants, skipListener))
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
    public Step masterStepTvrRun(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepTvrRun", jobRepository)
                .partitioner(tvrJobStep(jobRepository,skipListener).getName(), partitionerTvrRun())
                .step(tvrJobStep(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepTvrRunError(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepTvrRunError", jobRepository)
                .partitioner(tvrJobErrorStep(jobRepository,skipListener).getName(), partitionerTvrRunRetry())
                .step(tvrJobErrorStep(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepTvrRunErrorRetry(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepTvrRunErrorRetry", jobRepository)
                .partitioner(tvrJobErrorRetryStep(jobRepository,skipListener).getName(), partitionerTvrRunRetry())
                .step(tvrJobErrorRetryStep(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
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
    public Step tvrJobStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("tvrJobStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderTvrRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step tvrJobErrorStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("tvrJobErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderTvrErrorRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step tvrJobErrorRetryStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("tvrJobErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderTvrErrorRetryRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean(name="tvrBatchJob")
    public Job tvrBatchJob(JobRepository jobRepository, TvrRunJobCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("tvrBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepTvrRun(jobRepository,constants,skipListener))
                .on("*")
                .to(masterStepTvrRunError(jobRepository,constants,skipListener))
                .on("*")
                .to(masterStepTvrRunErrorRetry(jobRepository,constants,skipListener))
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
    public Step masterStepSpcRegGrad(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcRegGrad", jobRepository)
                .partitioner(slaveSpcRegGradStep(jobRepository,skipListener).getName(), partitionerSpcRegGrad())
                .step(slaveSpcRegGradStep(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepSpcRegGradError(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcRegGradError", jobRepository)
                .partitioner(slaveSpcRegGradErrorStep(jobRepository,skipListener).getName(), partitionerSpcRegGradRetry())
                .step(slaveSpcRegGradErrorStep(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepSpcRegGradErrorRetry(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcRegGradErrorRetry", jobRepository)
                .partitioner(slaveSpcRegGradErrorRetryStep(jobRepository,skipListener).getName(), partitionerSpcRegGradRetry())
                .step(slaveSpcRegGradErrorRetryStep(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
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
    public Step slaveSpcRegGradStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("slaveStepSpcRegGrad", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step slaveSpcRegGradErrorStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("slaveSpcRegGradErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step slaveSpcRegGradErrorRetryStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("slaveSpcRegGradErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean(name="SpecialGraduationBatchJob")
    public Job specialGraduationBatchJob(JobRepository jobRepository, SpecialRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("SpecialGraduationBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepSpcRegGrad(jobRepository,constants,skipListener))
                .on("*")
                .to(masterStepSpcRegGradError(jobRepository,constants,skipListener))
                .on("*")
                .to(masterStepSpcRegGradErrorRetry(jobRepository,constants,skipListener))
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
    public Step masterStepSpcTvrRun(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcTvrRun", jobRepository)
                .partitioner(slaveStepSpcTvrRun(jobRepository,skipListener).getName(), partitionerSpcRegGrad())
                .step(slaveStepSpcTvrRun(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepSpcTvrRunError(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcTvrRunError", jobRepository)
                .partitioner(slaveStepSpcTvrRunError(jobRepository,skipListener).getName(), partitionerSpcRegGradRetry())
                .step(slaveStepSpcTvrRunError(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepSpcTvrRunErrorRetry(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepSpcTvrRunErrorRetry", jobRepository)
                .partitioner(slaveStepSpcTvrRunErrorRetry(jobRepository,skipListener).getName(), partitionerSpcRegGradRetry())
                .step(slaveStepSpcTvrRunErrorRetry(jobRepository,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRun(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("slaveStepSpcTvrRun", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRunError(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("slaveStepSpcTvrRunError", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRunErrorRetry(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("slaveStepSpcTvrRunErrorRetry", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean(name="SpecialTvrRunBatchJob")
    public Job specialTvrRunBatchJob(JobRepository jobRepository, SpecialRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("SpecialTvrRunBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepSpcTvrRun(jobRepository,constants,skipListener))
                .on("*")
                .to(masterStepSpcTvrRunError(jobRepository,constants,skipListener))
                .on("*")
                .to(masterStepSpcTvrRunErrorRetry(jobRepository,constants,skipListener))
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
    public Step slaveStepDisRun(JobRepository jobRepository) {
        return new StepBuilder("slaveStepDisRun", jobRepository)
                .<StudentCredentialDistribution, StudentCredentialDistribution>chunk(1)
                .transactionManager(batchTransactionManager)
                .reader(itemReaderDisRun())
                .processor(itemProcessorDisRun())
                .writer(itemWriterDisRun())
                .transactionManager(batchTransactionManager)
                .build();
    }

    @Bean
    public Step slaveStepDisRunYearly(JobRepository jobRepository) {
        return new StepBuilder("slaveStepDisRun", jobRepository)
                .<StudentCredentialDistribution, StudentCredentialDistribution>chunk(1)
                .transactionManager(batchTransactionManager)
                .reader(itemReaderDisRun())
                .processor(itemProcessorDisRun())
                .writer(itemWriterDisRun())
                .transactionManager(batchTransactionManager)
                .build();
    }

    @Bean
    public Step slaveStepDisRunYearlyNonGradByMincode(JobRepository jobRepository) {
        return new StepBuilder("slaveStepDisRunYearlyNonGrad", jobRepository)
                .<String, List<StudentCredentialDistribution>>chunk(1)
                .transactionManager(batchTransactionManager)
                .reader(itemReaderDisRunYearlyNonGrad())
                .processor(itemProcessorDisRunYearlyNonGradByMincode())
                .writer(itemWriterDisRunYearlyNonGrad())
                .transactionManager(batchTransactionManager)
                .build();
    }

    @Bean
    public Step masterStepDisRun(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDisRun", jobRepository)
                .partitioner(slaveStepDisRun(jobRepository).getName(), partitionerDisRun())
                .step(slaveStepDisRun(jobRepository))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean(name="DistributionBatchJob")
    public Job distributionBatchJob(JobRepository jobRepository, DistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("DistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRun(jobRepository,constants))
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
    public Step masterStepDisRunYearly(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDisRunYearly", jobRepository)
                .partitioner(slaveStepDisRun(jobRepository).getName(), partitionerDisRunYearly())
                .step(slaveStepDisRunYearly(jobRepository))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean(name="YearlyDistributionBatchJob")
    public Job distributionBatchJobYearly(JobRepository jobRepository, DistributionRunYearlyCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("YearlyDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRunYearly(jobRepository,constants))
                .end()
                .build();
    }

    @Bean
    @StepScope
    public DistributionRunYearlyNonGradPartitioner partitionerDisRunYearlyNonGrad() {
        return new DistributionRunYearlyNonGradPartitioner();
    }

    @Bean
    public Step masterStepDisRunYearlyNonGrad(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDisRunYearlyNonGrad", jobRepository)
                .partitioner(slaveStepDisRunYearlyNonGradByMincode(jobRepository).getName(), partitionerDisRunYearlyNonGrad())
                .step(slaveStepDisRunYearlyNonGradByMincode(jobRepository))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean(name="YearlyNonGradDistributionBatchJob")
    public Job distributionBatchJobYearlyNonGrad(JobRepository jobRepository, DistributionRunYearlyNonGradCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("YearlyNonGradDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRunYearlyNonGrad(jobRepository,constants))
                .end()
                .build();
    }

    @Bean
    @StepScope
    public DistributionRunSupplementalPartitioner partitionerDisRunSupplemental() {
        return new DistributionRunSupplementalPartitioner();
    }

    @Bean
    public Step masterStepDisRunSupplemental(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDisRunSupplemental", jobRepository)
                .partitioner(slaveStepDisRun(jobRepository).getName(), partitionerDisRunSupplemental())
                .step(slaveStepDisRun(jobRepository))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean(name="SupplementalDistributionBatchJob")
    public Job distributionBatchJobSupplemental(JobRepository jobRepository, DistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("SupplementalDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRunSupplemental(jobRepository,constants))
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
    public Step masterStepUserReqDisRun(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepUserReqDisRun", jobRepository)
                .partitioner(slaveStepDisRun(jobRepository).getName(), partitionerDisRunUserReq())
                .step(slaveStepDisRun(jobRepository))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean(name="UserReqDistributionBatchJob")
    public Job distributionBatchJobUserReq(JobRepository jobRepository, UserReqDistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("UserReqDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepUserReqDisRun(jobRepository,constants))
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
    public Step slaveStepBlankDisRun(JobRepository jobRepository) {
        return new StepBuilder("slaveStepBlankDisRun", jobRepository)
                .<BlankCredentialDistribution, BlankCredentialDistribution>chunk(1)
                .transactionManager(batchTransactionManager)
                .reader(itemReaderBlankDisRun())
                .processor(itemProcessorBlankDisRun())
                .writer(itemWriterBlankDisRun())
                .build();
    }

    @Bean
    public Step masterStepBlankUserReqDisRun(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepBlankUserReqDisRun", jobRepository)
                .partitioner(slaveStepBlankDisRun(jobRepository).getName(), partitionerDisRunBlankUserReq())
                .step(slaveStepBlankDisRun(jobRepository))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }


    @Bean(name="blankDistributionBatchJob")
    public Job blankDistributionBatchJobUserReq(JobRepository jobRepository, UserReqBlankDistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("blankDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepBlankUserReqDisRun(jobRepository,constants))
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
    public Step slaveStepPsiDisRun(JobRepository jobRepository) {
        return new StepBuilder("slaveStepPsiDisRun", jobRepository)
                .<PsiCredentialDistribution, PsiCredentialDistribution>chunk(1)
                .transactionManager(batchTransactionManager)
                .reader(itemReaderPsiDisRun())
                .processor(itemProcessorPsiDisRun())
                .writer(itemWriterPsiDisRun())
                .build();
    }

    @Bean
    public Step masterStepPsiUserReqDisRun(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepPsiUserReqDisRun", jobRepository)
                .partitioner(slaveStepPsiDisRun(jobRepository).getName(), partitionerDisRunPsiUserReq())
                .step(slaveStepPsiDisRun(jobRepository))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }


    @Bean(name="psiDistributionBatchJob")
    public Job psiDistributionBatchJobUserReq(JobRepository jobRepository, UserReqPsiDistributionRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("psiDistributionBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepPsiUserReqDisRun(jobRepository,constants))
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
    public Step masterStepCertRegen(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepCertRegen", jobRepository)
                .partitioner(certRegenJobStep(jobRepository, skipListener).getName(), partitionerCertRegen())
                .step(certRegenJobStep(jobRepository, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step certRegenJobStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("certRegenJobStep", jobRepository)
                .<UUID, Integer>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderCertRegen())
                .processor(itemProcessorCertRegen())
                .writer(itemWriterCertRegen())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean(name="certRegenBatchJob")
    public Job certRegenBatchJob(JobRepository jobRepository, RegenCertRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("certRegenBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepCertRegen(jobRepository,constants, skipListener))
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
    public Step masterStepEdwSnapshotSchool(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepEdwSnapshotSchool", jobRepository)
                .partitioner(edwSnapshotSchoolJobStep(jobRepository, skipListener).getName(), partitionerEDWSnapshotSchool())
                .step(edwSnapshotSchoolJobStep(jobRepository, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step edwSnapshotSchoolJobStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("edwSnapshotSchoolJobStep", jobRepository)
                .<String, List<Pair<String, List<SnapshotResponse>>>>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderEDWSnapshotSchool())
                .processor(itemProcessorEDWSnapshotSchool())
                .writer(itemWriterEDWSnapshotSchool())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
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
    public Step masterStepEdwSnapshot(JobRepository jobRepository, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("masterStepEdwSnapshot", jobRepository)
                .partitioner(edwSnapshotJobStep(jobRepository, skipListener).getName(), partitionerEDWSnapshot())
                .step(edwSnapshotJobStep(jobRepository, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step edwSnapshotJobStep(JobRepository jobRepository, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("edwSnapshotJobStep", jobRepository)
                .<SnapshotResponse, EdwGraduationSnapshot>chunk(1)
                .transactionManager(batchTransactionManager)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderEDWSnapshot())
                .processor(itemProcessorEDWSnapshot())
                .writer(itemWriterEDWSnapshot())
                .transactionManager(batchTransactionManager)
                .listener(skipListener)
                .build();
    }

    @Bean(name="edwSnapshotBatchJob")
    public Job edwSnapshotBatchJob(JobRepository jobRepository, EdwSnapshotCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("edwSnapshotBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepEdwSnapshotSchool(jobRepository,constants, skipListener))
                .on("*")
                .to(masterStepEdwSnapshot(jobRepository,constants, skipListener))
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
    public Step slaveStepUserScheduled(JobRepository jobRepository) {
        return new StepBuilder("slaveStepUserScheduled", jobRepository)
                .<UserScheduledJobs, UserScheduledJobs>chunk(1)
                .transactionManager(batchTransactionManager)
                .reader(itemReaderUserScheduled())
                .processor(itemProcessorUserScheduled())
                .writer(itemWriterUserScheduled())
                .build();
    }

    @Bean(name="userScheduledBatchJobRefresher")
    public Job userScheduledBatchJobQueueRefresher(JobRepository jobRepository, UserScheduledCompletionNotificationListener listener) {
        return new JobBuilder("userScheduledBatchJobRefresher", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(slaveStepUserScheduled(jobRepository))
                .end()
                .build();
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }

    @Bean
    public TaskExecutor taskExecutor(EducGradBatchGraduationApiConstants constants) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(constants.getNumberOfPartitions());
        executor.setMaxPoolSize(constants.getNumberOfPartitions());
        executor.setThreadNamePrefix("partition-");
        executor.initialize();
        return executor;
    }
}