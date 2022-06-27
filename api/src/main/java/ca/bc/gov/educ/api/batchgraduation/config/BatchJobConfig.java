package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.*;
import ca.bc.gov.educ.api.batchgraduation.model.BlankCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.processor.*;
import ca.bc.gov.educ.api.batchgraduation.reader.*;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.writer.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    // Partitioning for Regular Grad Run updates

    @Autowired
    JobRegistry jobRegistry;

    @Bean
    @StepScope
    public ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> itemProcessorRegGrad() {
        return new RunRegularGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<GraduationStudentRecord> itemReaderRegGrad() {
        return new RecalculateStudentReader();
    }

    @Bean
    @StepScope
    public ItemReader<GraduationStudentRecord> itemReaderRegErrorGrad() {
        return new RecalculateStudentErrorReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterRegGrad() {
        return new RegGradAlgBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepRegGrad(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepRegGrad")
                .partitioner(graduationJobStep(stepBuilderFactory).getName(), partitionerRegGrad())
                .step(graduationJobStep(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    public Step masterStepErrorRegGrad(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepErrorRegGrad")
                .partitioner(graduationJobErrorStep(stepBuilderFactory).getName(), partitionerRegGrad())
                .step(graduationJobErrorStep(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    public Step masterStepErrorRegGradRetry(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepErrorRegGradRetry")
                .partitioner(graduationJobErrorStep(stepBuilderFactory).getName(), partitionerRegGrad())
                .step(graduationJobErrorStep(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }


    @Bean
    public RegGradAlgPartitioner partitionerRegGrad() {
        return new RegGradAlgPartitioner();
    }

    @Bean
    public Step graduationJobErrorStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("graduationJobErrorStep")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReaderRegErrorGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .build();
    }

    @Bean
    public Step graduationJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("graduationJobStep")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReaderRegGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="GraduationBatchJob")
    public Job graduationBatchJob(GradRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory,JobBuilderFactory jobBuilderFactory,EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("GraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepRegGrad(stepBuilderFactory,constants))
                .next(masterStepErrorRegGrad(stepBuilderFactory,constants))
                .next(masterStepErrorRegGradRetry(stepBuilderFactory,constants))
                .build();
    }


    // Partitioning for Regular TVR Run updates

    @Bean
    @StepScope
    public ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> itemProcessorTvrRun() {
        return new RunProjectedGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<GraduationStudentRecord> itemReaderTvrRun() {
        return new RecalculateProjectedGradRunReader();
    }

    @Bean
    @StepScope
    public ItemReader<GraduationStudentRecord> itemReaderTvrErrorRun() {
        return new RecalculateProjectedGradRunErrorReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterTvrRun() {
        return new TvrRunBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepTvrRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepTvrRun")
                .partitioner(tvrJobStep(stepBuilderFactory).getName(), partitionerTvrRun())
                .step(tvrJobStep(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    public Step masterStepErrorTvrRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepErrorTvrRun")
                .partitioner(tvrJobErrorStep(stepBuilderFactory).getName(), partitionerTvrRun())
                .step(tvrJobErrorStep(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    public Step masterStepErrorTvrRunRetry(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepErrorTvrRunRetry")
                .partitioner(tvrJobErrorStep(stepBuilderFactory).getName(), partitionerTvrRun())
                .step(tvrJobErrorStep(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    public TvrRunPartitioner partitionerTvrRun() {
        return new TvrRunPartitioner();
    }


    @Bean
    public Step tvrJobStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("tvrJobStep")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReaderTvrRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .build();
    }

    @Bean
    public Step tvrJobErrorStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("tvrJobErrorStep")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReaderTvrErrorRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="tvrBatchJob")
    public Job tvrBatchJob(TvrRunJobCompletionNotificationListener listener,StepBuilderFactory stepBuilderFactory,JobBuilderFactory jobBuilderFactory,EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("tvrBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepTvrRun(stepBuilderFactory,constants))
                .next(masterStepErrorTvrRun(stepBuilderFactory,constants))
                .next(masterStepErrorTvrRunRetry(stepBuilderFactory,constants))
                .build();
    }

    //
    @Bean
    @StepScope
    public ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> itemProcessorSpcRegGrad() {
        return new RunSpecialGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<GraduationStudentRecord> itemReaderSpcRegGrad() {
        return new SpecialProjectedGradRunReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterSpcRegGrad() {
        return new TvrRunBatchPerformanceWriter();
    }

    @Bean
    @StepScope
    public ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> itemProcessorSpcTvrRun() {
        return new RunSpecialProjectedGradAlgorithmProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<GraduationStudentRecord> itemReaderSpcTvrRun() {
        return new SpecialGradRunStudentReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterSpcTvrRun() {
        return new RegGradAlgBatchPerformanceWriter();
    }

    // Partitioning for Regular Grad Run updates
    @Bean
    public Step masterStepSpcRegGrad(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepSpcRegGrad")
                .partitioner(slaveStepSpcRegGrad(stepBuilderFactory).getName(), partitionerSpcRegGrad())
                .step(slaveStepSpcRegGrad(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    public Step masterStepSpcTvrRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepSpcTvrRun")
                .partitioner(slaveStepSpcTvrRun(stepBuilderFactory).getName(), partitionerSpcRegGrad())
                .step(slaveStepSpcTvrRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    @StepScope
    public SpcRegGradAlgPartitioner partitionerSpcRegGrad() {
        return new SpcRegGradAlgPartitioner();
    }


    @Bean
    public Step slaveStepSpcRegGrad(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepSpcRegGrad")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .build();
    }

    @Bean
    public Step slaveStepSpcTvrRun(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepSpcTvrRun")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="SpecialGraduationBatchJob")
    public Job specialGraduationBatchJob(SpecialRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("SpecialGraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepSpcRegGrad(stepBuilderFactory,constants))
                .end()
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="SpecialTvrRunBatchJob")
    public Job specialTvrRunBatchJob(SpecialRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("SpecialTvrRunBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepSpcTvrRun(stepBuilderFactory,constants))
                .end()
                .build();
    }

    //

    //
    @Bean
    @StepScope
    public ItemProcessor<StudentCredentialDistribution,StudentCredentialDistribution> itemProcessorDisRun() {
        return new DistributionRunProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<StudentCredentialDistribution> itemReaderDisRun() {
        return new DistributionRunReader();
    }

    @Bean
    @StepScope
    public ItemWriter<StudentCredentialDistribution> itemWriterDisRun() {
        return new DistributionRunWriter();
    }

    //
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
    // Partitioning for Regular Grad Run updates
    @Bean
    public Step masterStepDisRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepDisRun")
                .partitioner(slaveStepDisRun(stepBuilderFactory).getName(), partitionerDisRun())
                .step(slaveStepDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    @StepScope
    public DistributionRunPartitioner partitionerDisRun() {
        return new DistributionRunPartitioner();
    }


    @Bean
    public Step slaveStepDisRun(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepDisRun")
                .<StudentCredentialDistribution, StudentCredentialDistribution>chunk(1)
                .reader(itemReaderDisRun())
                .processor(itemProcessorDisRun())
                .writer(itemWriterDisRun())
                .build();
    }

    @Bean
    public Step slaveStepBlankDisRun(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepBlankDisRun")
                .<BlankCredentialDistribution, BlankCredentialDistribution>chunk(1)
                .reader(itemReaderBlankDisRun())
                .processor(itemProcessorBlankDisRun())
                .writer(itemWriterBlankDisRun())
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="DistributionBatchJob")
    public Job distributionBatchJob(DistributionRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("DistributionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRun(stepBuilderFactory,constants))
                .end()
                .build();
    }

    //

    // Partitioning for Regular Grad Run updates
    @Bean
    public Step masterStepDisRunYearly(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepDisRunYearly")
                .partitioner(slaveStepDisRun(stepBuilderFactory).getName(), partitionerDisRunYearly())
                .step(slaveStepDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    public Step masterStepUserReqDisRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepUserReqDisRun")
                .partitioner(slaveStepDisRun(stepBuilderFactory).getName(), partitionerDisRunUserReq())
                .step(slaveStepDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    public Step masterStepBlankUserReqDisRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepBlankUserReqDisRun")
                .partitioner(slaveStepBlankDisRun(stepBuilderFactory).getName(), partitionerDisRunBlankUserReq())
                .step(slaveStepBlankDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    @StepScope
    public DistributionRunPartitionerYearly partitionerDisRunYearly() {
        return new DistributionRunPartitionerYearly();
    }


    @Bean
    @StepScope
    public DistributionRunPartitionerUserReq partitionerDisRunUserReq() {
        return new DistributionRunPartitionerUserReq();
    }

    @Bean
    @StepScope
    public DistributionRunPartitionerBlankUserReq partitionerDisRunBlankUserReq() {
        return new DistributionRunPartitionerBlankUserReq();
    }

    @Bean
    public Step slaveStepDisRunYearly(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepDisRun")
                .<StudentCredentialDistribution, StudentCredentialDistribution>chunk(1)
                .reader(itemReaderDisRun())
                .processor(itemProcessorDisRun())
                .writer(itemWriterDisRun())
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="YearlyDistributionBatchJob")
    public Job distributionBatchJobYearly(DistributionRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("YearlyDistributionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRunYearly(stepBuilderFactory,constants))
                .end()
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="UserReqDistributionBatchJob")
    public Job distributionBatchJobUserReq(UserReqDistributionRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("UserReqDistributionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepUserReqDisRun(stepBuilderFactory,constants))
                .end()
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="blankDistributionBatchJob")
    public Job blankDistributionBatchJobUserReq(UserReqBlankDistributionRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("blankDistributionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepBlankUserReqDisRun(stepBuilderFactory,constants))
                .end()
                .build();
    }

    //
    @Bean
    @StepScope
    public ItemProcessor<SchoolReportDistribution,SchoolReportDistribution> itemProcessorSchoolReportRun() {
        return new SchoolReportRunProcessor();
    }

    @Bean
    @StepScope
    public ItemReader<SchoolReportDistribution> itemReaderSchoolReportRun() {
        return new SchoolReportRunReader();
    }

    @Bean
    @StepScope
    public ItemWriter<SchoolReportDistribution> itemWriterSchoolReportRun() {
        return new SchoolReportRunWriter();
    }


    // Partitioning for Regular Grad Run updates
    @Bean
    public Step masterStepSchoolReportRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepSchoolReportRun")
                .partitioner(slaveStepSchoolReportRun(stepBuilderFactory).getName(), partitionerSchoolReportRun())
                .step(slaveStepSchoolReportRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants.getNumberOfPartitions()))
                .build();
    }

    @Bean
    @StepScope
    public SchoolReportRunPartitioner partitionerSchoolReportRun() {
        return new SchoolReportRunPartitioner();
    }


    @Bean
    public Step slaveStepSchoolReportRun(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepSchoolReportRun")
                .<SchoolReportDistribution, SchoolReportDistribution>chunk(1)
                .reader(itemReaderSchoolReportRun())
                .processor(itemProcessorSchoolReportRun())
                .writer(itemWriterSchoolReportRun())
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="SchoolReportBatchJob")
    public Job schoolReportBatchJob(SchoolReportRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("SchoolReportBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepSchoolReportRun(stepBuilderFactory,constants))
                .end()
                .build();
    }

    //

    //
    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }

    @Bean
    public TaskExecutor taskExecutor(int numberOfPartitions) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(numberOfPartitions);
        executor.setMaxPoolSize(numberOfPartitions);
        executor.setThreadNamePrefix("partition_task_executor_thread-");
        executor.initialize();
        return executor;
    }
}
