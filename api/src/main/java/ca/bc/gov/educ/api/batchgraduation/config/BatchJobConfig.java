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

    /**
     * Regular Grad Algorithm Run
     * ItemProcessor,ItemReader and ItemWriter
     * Partitioner
     */

    @Bean
    @StepScope
    public RunRegularGradAlgorithmProcessor itemProcessorRegGrad() {
        return new RunRegularGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public RecalculateStudentReader itemReaderRegGrad() {
        return new RecalculateStudentReader();
    }

    @Bean
    @StepScope
    public RecalculateStudentErrorReader itemReaderRegErrorGrad() {
        return new RecalculateStudentErrorReader();
    }

    @Bean
    @StepScope
    public RecalculateStudentErrorRetryReader itemReaderRegErrorRetryGrad() {
        return new RecalculateStudentErrorRetryReader();
    }


    @Bean
    @StepScope
    public RegGradAlgBatchPerformanceWriter itemWriterRegGrad() {
        return new RegGradAlgBatchPerformanceWriter();
    }

    @Bean
    @StepScope
    public ArchiveStudentsProcessor archiveStudentsProcessor() {
        return new ArchiveStudentsProcessor();
    }

    @Bean
    @StepScope
    public ArchiveStudentsReader archiveStudentsReader() {
        return new ArchiveStudentsReader();
    }

    @Bean
    @StepScope
    public ItemWriter<? super List<String>> archiveStudentsWriter() {
        return new ArchiveStudentsWriter();
    }

    @Bean
    @StepScope
    public ArchiveSchoolReportsProcessor archiveSchoolReportsProcessor() {
        return new ArchiveSchoolReportsProcessor();
    }

    @Bean
    @StepScope
    public DeleteStudentReportsProcessor deleteStudentReportsProcessor() {
        return new DeleteStudentReportsProcessor();
    }

    @Bean
    @StepScope
    public ArchiveSchoolReportsReader archiveSchoolReportsReader() {
        return new ArchiveSchoolReportsReader();
    }

    @Bean
    @StepScope
    public DeleteStudentReportsReader deleteStudentReportsReader() {
        return new DeleteStudentReportsReader();
    }

    @Bean
    @StepScope
    public ItemWriter<? super List<String>> archiveSchoolReportsWriter() {
        return new ArchiveSchoolReportsWriter();
    }

    @Bean
    @StepScope
    public ItemWriter<? super List<UUID>> deleteStudentReportsWriter() {
        return new DeleteStudentReportsWriter();
    }

    @Bean
    public Step masterStepRegGrad(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = graduationJobStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepRegGrad", jobRepository)
                .partitioner(step.getName(), partitionerRegGrad())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepArchiveStudents(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepArchiveStudents", jobRepository)
                .partitioner("archiveStudentsPartitioner", partitionerArchiveStudents())
                .step(archiveStudentsJobStep(jobRepository, transactionManager, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step archiveStudentsJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("archiveStudentsJobStep", jobRepository)
                .<List<String>, List<String>>chunk(1, transactionManager)
                .processor(archiveStudentsProcessor())
                .reader(archiveStudentsReader())
                .writer(archiveStudentsWriter())
                .build();
    }

    @Bean
    public Step masterStepArchiveSchoolReports(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepArchiveSchoolReports", jobRepository)
                .partitioner("archiveSchoolReportsPartitioner", partitionerArchiveSchoolReports())
                .step(archiveSchoolReportsJobStep(jobRepository, transactionManager, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepDeleteStudentReports(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        return new StepBuilder("masterStepDeleteStudentReports", jobRepository)
                .partitioner("deleteStudentReportsPartitioner", partitionerDeleteStudentReports())
                .step(deleteStudentReportsJobStep(jobRepository, transactionManager, constants))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step archiveSchoolReportsJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("archiveSchoolReportsJobStep", jobRepository)
                .<List<String>, List<String>>chunk(1, transactionManager)
                .reader(archiveSchoolReportsReader())
                .processor(archiveSchoolReportsProcessor())
                .writer(archiveSchoolReportsWriter())
                .build();
    }

    @Bean
    public Step deleteStudentReportsJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("deleteStudentReportsJobStep", jobRepository)
                .<List<UUID>, List<UUID>>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(deleteStudentReportsReader())
                .processor(deleteStudentReportsProcessor())
                .writer(deleteStudentReportsWriter())
                .build();
    }

    @Bean
    public Step masterStepRegGradError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = graduationJobErrorStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepRegGradError", jobRepository)
                .partitioner(step.getName(), partitionerRegGradRetry())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepRegGradErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = graduationJobErrorRetryStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepRegGradErrorRetry", jobRepository)
                .partitioner(step.getName(), partitionerRegGradRetry())
                .step(step)
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
    public ArchiveSchoolReportsPartitioner partitionerArchiveSchoolReports() {
        return new ArchiveSchoolReportsPartitioner();
    }

    @Bean
    @StepScope
    public DeleteStudentReportsPartitioner partitionerDeleteStudentReports() {
        return new DeleteStudentReportsPartitioner();
    }

    @Bean
    @StepScope
    public ArchiveStudentsPartitioner partitionerArchiveStudents() {
        return new ArchiveStudentsPartitioner();
    }

    @Bean
    @StepScope
    public RegGradAlgPartitionerRetry partitionerRegGradRetry() {
        return new RegGradAlgPartitionerRetry();
    }

    @Bean
    public Step graduationJobErrorStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("graduationJobErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderRegErrorGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .build();
    }

    @Bean
    public Step graduationJobErrorRetryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("graduationJobErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderRegErrorRetryGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .build();
    }

    @Bean
    public Step graduationJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("graduationJobStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderRegGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .build();
    }

    @Bean(name="GraduationBatchJob")
    public Job graduationBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, GradRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("GraduationBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepRegGrad(jobRepository, transactionManager, constants))
                .on("*")
                .to(masterStepRegGradError(jobRepository, transactionManager,constants))
                .on("*")
                .to(masterStepRegGradErrorRetry(jobRepository, transactionManager,constants))
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
    public RunProjectedGradAlgorithmProcessor itemProcessorTvrRun() {
        return new RunProjectedGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public RecalculateProjectedGradRunReader itemReaderTvrRun() {
        return new RecalculateProjectedGradRunReader();
    }

    @Bean
    @StepScope
    public RecalculateProjectedGradRunErrorReader itemReaderTvrErrorRun() {
        return new RecalculateProjectedGradRunErrorReader();
    }

    @Bean
    @StepScope
    public RecalculateProjectedGradRunErrorRetryReader itemReaderTvrErrorRetryRun() {
        return new RecalculateProjectedGradRunErrorRetryReader();
    }

    @Bean
    @StepScope
    public TvrRunBatchPerformanceWriter itemWriterTvrRun() {
        return new TvrRunBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepTvrRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = tvrJobStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepTvrRun", jobRepository)
                .partitioner(step.getName(), partitionerTvrRun())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepTvrRunError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = tvrJobErrorStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepTvrRunError", jobRepository)
                .partitioner(step.getName(), partitionerTvrRunRetry())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepTvrRunErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = tvrJobErrorRetryStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepTvrRunErrorRetry", jobRepository)
                .partitioner(step.getName(), partitionerTvrRunRetry())
                .step(step)
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
    public Step tvrJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("tvrJobStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderTvrRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .build();
    }

    @Bean
    public Step tvrJobErrorStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("tvrJobErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderTvrErrorRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .build();
    }

    @Bean
    public Step tvrJobErrorRetryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("tvrJobErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderTvrErrorRetryRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .build();
    }

    @Bean(name="tvrBatchJob")
    public Job tvrBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, TvrRunJobCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("tvrBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepTvrRun(jobRepository, transactionManager,constants))
                .on("*")
                .to(masterStepTvrRunError(jobRepository, transactionManager,constants))
                .on("*")
                .to(masterStepTvrRunErrorRetry(jobRepository, transactionManager,constants))
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
    public RunSpecialGradAlgorithmProcessor itemProcessorSpcRegGrad() {
        return new RunSpecialGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public SpecialGradRunStudentReader itemReaderSpcRegGrad() {
        return new SpecialGradRunStudentReader();
    }

    @Bean
    @StepScope
    public RegGradAlgBatchPerformanceWriter itemWriterSpcRegGrad() {
        return new RegGradAlgBatchPerformanceWriter();
    }



    @Bean
    public Step masterStepSpcRegGrad(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = slaveSpcRegGradStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepSpcRegGrad", jobRepository)
                .partitioner(step.getName(), partitionerSpcRegGrad())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepSpcRegGradError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = slaveSpcRegGradErrorStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepSpcRegGradError", jobRepository)
                .partitioner(step.getName(), partitionerSpcRegGradRetry())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepSpcRegGradErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = slaveSpcRegGradErrorRetryStep(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepSpcRegGradErrorRetry", jobRepository)
                .partitioner(step.getName(), partitionerSpcRegGradRetry())
                .step(step)
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
    public Step slaveSpcRegGradStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveStepSpcRegGrad", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .build();
    }

    @Bean
    public Step slaveSpcRegGradErrorStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveSpcRegGradErrorStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .build();
    }

    @Bean
    public Step slaveSpcRegGradErrorRetryStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveSpcRegGradErrorRetryStep", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .build();
    }

    @Bean(name="SpecialGraduationBatchJob")
    public Job specialGraduationBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, SpecialRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("SpecialGraduationBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepSpcRegGrad(jobRepository, transactionManager,constants))
                .on("*")
                .to(masterStepSpcRegGradError(jobRepository, transactionManager,constants))
                .on("*")
                .to(masterStepSpcRegGradErrorRetry(jobRepository, transactionManager,constants))
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
    public RunSpecialProjectedGradAlgorithmProcessor itemProcessorSpcTvrRun() {
        return new RunSpecialProjectedGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public SpecialProjectedGradRunReader itemReaderSpcTvrRun() {
        return new SpecialProjectedGradRunReader();
    }

    @Bean
    @StepScope
    public TvrRunBatchPerformanceWriter itemWriterSpcTvrRun() {
        return new TvrRunBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepSpcTvrRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = slaveStepSpcTvrRun(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepSpcTvrRun", jobRepository)
                .partitioner(step.getName(), partitionerSpcRegGrad())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepSpcTvrRunError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = slaveStepSpcTvrRunError(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepSpcTvrRunError", jobRepository)
                .partitioner(step.getName(), partitionerSpcRegGradRetry())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step masterStepSpcTvrRunErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants) {
        Step step = slaveStepSpcTvrRunErrorRetry(jobRepository, transactionManager, constants);
        return new StepBuilder("masterStepSpcTvrRunErrorRetry", jobRepository)
                .partitioner(step.getName(), partitionerSpcRegGradRetry())
                .step(step)
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveStepSpcTvrRun", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRunError(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveStepSpcTvrRunError", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRunErrorRetry(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constant) {
        return new StepBuilder("slaveStepSpcTvrRunErrorRetry", jobRepository)
                .<UUID, GraduationStudentRecord>chunk(constant.getTransactionChunkSize(), transactionManager)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .build();
    }

    @Bean(name="SpecialTvrRunBatchJob")
    public Job specialTvrRunBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, SpecialRunCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("SpecialTvrRunBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepSpcTvrRun(jobRepository, transactionManager,constants))
                .on("*")
                .to(masterStepSpcTvrRunError(jobRepository, transactionManager,constants))
                .on("*")
                .to(masterStepSpcTvrRunErrorRetry(jobRepository, transactionManager,constants))
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
    public DistributionRunProcessor itemProcessorDisRun() {
        return new DistributionRunProcessor();
    }

    @Bean
    @StepScope
    public DistributionRunYearlyProcessor itemProcessorYearlyDisRun() {
        return new DistributionRunYearlyProcessor();
    }

    @Bean
    @StepScope
    public DistributionRunYearlyNonGradProcessor itemProcessorDisRunYearlyNonGradByMincode() {
        return new DistributionRunYearlyNonGradProcessor();
    }

    @Bean
    @StepScope
    public DistributionRunStudentCredentialsReader itemReaderDisRun() {
        return new DistributionRunStudentCredentialsReader();
    }

    @Bean
    @StepScope
    public DistributionRunYearlyNonGradReader itemReaderDisRunYearlyNonGrad() {
        return new DistributionRunYearlyNonGradReader();
    }

    @Bean
    @StepScope
    public DistributionRunWriter itemWriterDisRun() {
        return new DistributionRunWriter();
    }

    @Bean
    @StepScope
    public DistributionRunYearlyNonGradWriter itemWriterDisRunYearlyNonGrad() {
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
                .processor(itemProcessorYearlyDisRun())
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
    public BlankDistributionRunProcessor itemProcessorBlankDisRun() {
        return new BlankDistributionRunProcessor();
    }

    @Bean
    @StepScope
    public BlankDistributionRunReader itemReaderBlankDisRun() {
        return new BlankDistributionRunReader();
    }

    @Bean
    @StepScope
    public BlankDistributionRunWriter itemWriterBlankDisRun() {
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
    public PsiDistributionRunProcessor itemProcessorPsiDisRun() {
        return new PsiDistributionRunProcessor();
    }

    @Bean
    @StepScope
    public PsiDistributionRunReader itemReaderPsiDisRun() {
        return new PsiDistributionRunReader();
    }

    @Bean
    @StepScope
    public PsiDistributionRunWriter itemWriterPsiDisRun() {
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
    public RegenerateCertificateReader itemReaderCertRegen() {
        return new RegenerateCertificateReader();
    }

    @Bean
    @StepScope
    public RegenerateCertificateRunWriter itemWriterCertRegen() {
        return new RegenerateCertificateRunWriter();
    }

    @Bean
    @StepScope
    public RunCertificateRegenerationProcessor itemProcessorCertRegen() {
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
                .<StudentCredentialDistribution, Integer>chunk(1, transactionManager)
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
     * Regenerate School Reports
     */

    @Bean
    @StepScope
    public RegenerateSchoolReportsPartitioner partitionerSchoolReportsRegen() {
        return new RegenerateSchoolReportsPartitioner();
    }

    @Bean
    @StepScope
    public RegenerateSchoolReportsReader itemReaderSchoolReportsRegen() {
        return new RegenerateSchoolReportsReader();
    }

    @Bean
    @StepScope
    public RegenerateSchoolReportsProcessor itemProcessorSchoolReportsRegen() { return new RegenerateSchoolReportsProcessor(); }

    @Bean
    @StepScope
    public RegenerateSchoolReportsWriter itemWriterSchoolReportsRegen() {
        return new RegenerateSchoolReportsWriter();
    }

    @Bean
    public Step schoolReportsRegenJobStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, SkipSQLTransactionExceptionsListener skipListener) {
        return new StepBuilder("schoolReportsRegenJobStep", jobRepository)
                .<String, String>chunk(1, transactionManager)
                .reader(itemReaderSchoolReportsRegen())
                .processor(itemProcessorSchoolReportsRegen())
                .writer(itemWriterSchoolReportsRegen())
                .faultTolerant()
                .listener(skipListener)
                .skip(Exception.class)
                .build();
    }

    @Bean
    public Step masterStepSchoolReportsRegen(JobRepository jobRepository, PlatformTransactionManager transactionManager, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        int partitionSize = constants.getNumberOfPartitions() / 2;
        return new StepBuilder("masterStepSchoolReportsRegen", jobRepository)
                .partitioner(schoolReportsRegenJobStep(jobRepository, transactionManager, skipListener).getName(), partitionerSchoolReportsRegen())
                .step(schoolReportsRegenJobStep(jobRepository, transactionManager, skipListener))
                .gridSize(partitionSize != 0? partitionSize : 1)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean(name="schoolReportsRegenBatchJob")
    public Job schoolReportsRegenBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, RegenSchoolReportsCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("schoolReportsRegenBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepSchoolReportsRegen(jobRepository, transactionManager,constants, skipListener))
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
    public EDWSnapshotSchoolReader itemReaderEDWSnapshotSchool() {
        return new EDWSnapshotSchoolReader();
    }

    @Bean
    @StepScope
    public EDWSnapshotSchoolWriter itemWriterEDWSnapshotSchool() {
        return new EDWSnapshotSchoolWriter();
    }

    @Bean
    @StepScope
    public EDWSnapshotSchoolProcessor itemProcessorEDWSnapshotSchool() {
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
    public EDWSnapshotReader itemReaderEDWSnapshot() {
        return new EDWSnapshotReader();
    }

    @Bean
    @StepScope
    public EDWSnapshotWriter itemWriterEDWSnapshot() {
        return new EDWSnapshotWriter();
    }

    @Bean
    @StepScope
    public EDWSnapshotProcessor itemProcessorEDWSnapshot() {
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

    @Bean(name="archiveSchoolReportsBatchJob")
    public Job archiveSchoolReportsBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, ArchiveSchoolReportsCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("archiveSchoolReportsBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepArchiveSchoolReports(jobRepository, transactionManager, constants))
                .on("*")
                .end().build()
                .build();
    }

    @Bean(name="deleteStudentReportsBatchJob")
    public Job deleteStudentReportsBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, DeleteStudentReportsCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("deleteStudentReportsBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepDeleteStudentReports(jobRepository, transactionManager, constants))
                .on("*")
                .end().build()
                .build();
    }

    @Bean(name="archiveStudentsBatchJob")
    public Job archiveStudentsBatchJob(JobRepository jobRepository, PlatformTransactionManager transactionManager, ArchiveStudentsCompletionNotificationListener listener, EducGradBatchGraduationApiConstants constants) {
        return new JobBuilder("archiveStudentsBatchJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepArchiveStudents(jobRepository, transactionManager, constants))
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