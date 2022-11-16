package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.*;
import ca.bc.gov.educ.api.batchgraduation.model.*;
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
import org.springframework.transaction.TransactionException;

import java.sql.SQLException;
import java.util.UUID;

@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    // Partitioning for Regular Grad Run updates

    @Autowired
    JobRegistry jobRegistry;

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
    public Step masterStepRegGrad(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("masterStepRegGrad")
                .partitioner(graduationJobStep(stepBuilderFactory, skipListener).getName(), partitionerRegGrad())
                .step(graduationJobStep(stepBuilderFactory, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepErrorRegGrad(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("masterStepErrorRegGrad")
                .partitioner(graduationJobErrorStep(stepBuilderFactory, skipListener).getName(), partitionerRegGradRetry())
                .step(graduationJobErrorStep(stepBuilderFactory, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepErrorRegGradRetry(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("masterStepErrorRegGradRetry")
                .partitioner(graduationJobErrorRetryStep(stepBuilderFactory, skipListener).getName(), partitionerRegGradRetry())
                .step(graduationJobErrorRetryStep(stepBuilderFactory, skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }


    @Bean
    @StepScope
    public RegGradAlgPartitioner partitionerRegGrad() {
        return new RegGradAlgPartitioner("Normal");
    }

    @Bean
    @StepScope
    public RegGradAlgPartitioner partitionerRegGradRetry() {
        return new RegGradAlgPartitioner("Retry");
    }

    @Bean
    public Step graduationJobErrorStep(StepBuilderFactory stepBuilderFactory, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("graduationJobErrorStep")
                .<UUID, GraduationStudentRecord>chunk(1)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderRegErrorGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step graduationJobErrorRetryStep(StepBuilderFactory stepBuilderFactory, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("graduationJobErrorRetryStep")
                .<UUID, GraduationStudentRecord>chunk(1)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderRegErrorRetryGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step graduationJobStep(StepBuilderFactory stepBuilderFactory, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("graduationJobStep")
                .<UUID, GraduationStudentRecord>chunk(1)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderRegGrad())
                .processor(itemProcessorRegGrad())
                .writer(itemWriterRegGrad())
                .listener(skipListener)
                .build();
    }

    @Bean(name="GraduationBatchJob")
    public Job graduationBatchJob(GradRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("GraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepRegGrad(stepBuilderFactory,constants, skipListener))
                .on("*")
                .to(masterStepErrorRegGrad(stepBuilderFactory,constants, skipListener))
                .on("*")
                .to(masterStepErrorRegGradRetry(stepBuilderFactory,constants, skipListener))
                .on("*").end()
                .build().build();
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
    public Step masterStepTvrRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("masterStepTvrRun")
                .partitioner(tvrJobStep(stepBuilderFactory,skipListener).getName(), partitionerTvrRun())
                .step(tvrJobStep(stepBuilderFactory,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepErrorTvrRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("masterStepErrorTvrRun")
                .partitioner(tvrJobErrorStep(stepBuilderFactory,skipListener).getName(), partitionerTvrRunRetry())
                .step(tvrJobErrorStep(stepBuilderFactory,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    public Step masterStepErrorTvrRunRetry(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("masterStepErrorTvrRunRetry")
                .partitioner(tvrJobErrorRetryStep(stepBuilderFactory,skipListener).getName(), partitionerTvrRunRetry())
                .step(tvrJobErrorRetryStep(stepBuilderFactory,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean
    @StepScope
    public TvrRunPartitioner partitionerTvrRun() {
        return new TvrRunPartitioner("Normal");
    }

    @Bean
    @StepScope
    public TvrRunPartitioner partitionerTvrRunRetry() {
        return new TvrRunPartitioner("Retry");
    }


    @Bean
    public Step tvrJobStep(StepBuilderFactory stepBuilderFactory, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("tvrJobStep")
                .<UUID, GraduationStudentRecord>chunk(1)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderTvrRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step tvrJobErrorStep(StepBuilderFactory stepBuilderFactory, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("tvrJobErrorStep")
                .<UUID, GraduationStudentRecord>chunk(1)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderTvrErrorRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step tvrJobErrorRetryStep(StepBuilderFactory stepBuilderFactory, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("tvrJobErrorRetryStep")
                .<UUID, GraduationStudentRecord>chunk(1)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderTvrErrorRetryRun())
                .processor(itemProcessorTvrRun())
                .writer(itemWriterTvrRun())
                .listener(skipListener)
                .build();
    }

    @Bean(name="tvrBatchJob")
    public Job tvrBatchJob(TvrRunJobCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("tvrBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(masterStepTvrRun(stepBuilderFactory,constants,skipListener))
                .on("*")
                .to(masterStepErrorTvrRun(stepBuilderFactory,constants,skipListener))
                .on("*")
                .to(masterStepErrorTvrRunRetry(stepBuilderFactory,constants,skipListener))
                .on("*").end()
                .build().build();
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
        return new SpecialProjectedGradRunReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterSpcRegGrad() {
        return new TvrRunBatchPerformanceWriter();
    }



    @Bean
    public Step masterStepSpcRegGrad(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("masterStepSpcRegGrad")
                .partitioner(slaveStepSpcRegGrad(stepBuilderFactory,skipListener).getName(), partitionerSpcRegGrad())
                .step(slaveStepSpcRegGrad(stepBuilderFactory,skipListener))
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
    public Step slaveStepSpcRegGrad(StepBuilderFactory stepBuilderFactory, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("slaveStepSpcRegGrad")
                .<UUID, GraduationStudentRecord>chunk(1)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderSpcRegGrad())
                .processor(itemProcessorSpcRegGrad())
                .writer(itemWriterSpcRegGrad())
                .listener(skipListener)
                .build();
    }

    @Bean(name="SpecialGraduationBatchJob")
    public Job specialGraduationBatchJob(SpecialRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("SpecialGraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepSpcRegGrad(stepBuilderFactory,constants,skipListener))
                .on("*")
                .end()
                .build().build();
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
        return new SpecialGradRunStudentReader();
    }

    @Bean
    @StepScope
    public ItemWriter<GraduationStudentRecord> itemWriterSpcTvrRun() {
        return new RegGradAlgBatchPerformanceWriter();
    }

    @Bean
    public Step slaveStepSpcTvrRun(StepBuilderFactory stepBuilderFactory, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("slaveStepSpcTvrRun")
                .<UUID, GraduationStudentRecord>chunk(1)
                .faultTolerant()
                .skip(SQLException.class)
                .skip(TransactionException.class)
                .reader(itemReaderSpcTvrRun())
                .processor(itemProcessorSpcTvrRun())
                .writer(itemWriterSpcTvrRun())
                .listener(skipListener)
                .build();
    }

    @Bean
    public Step masterStepSpcTvrRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants, SkipSQLTransactionExceptionsListener skipListener) {
        return stepBuilderFactory.get("masterStepSpcTvrRun")
                .partitioner(slaveStepSpcTvrRun(stepBuilderFactory,skipListener).getName(), partitionerSpcRegGrad())
                .step(slaveStepSpcTvrRun(stepBuilderFactory,skipListener))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean(name="SpecialTvrRunBatchJob")
    public Job specialTvrRunBatchJob(SpecialRunCompletionNotificationListener listener, SkipSQLTransactionExceptionsListener skipListener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("SpecialTvrRunBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepSpcTvrRun(stepBuilderFactory,constants,skipListener))
                .on("*")
                .end()
                .build().build();
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
    public ItemReader<StudentCredentialDistribution> itemReaderDisRun() {
        return new DistributionRunReader();
    }

    @Bean
    @StepScope
    public ItemWriter<StudentCredentialDistribution> itemWriterDisRun() {
        return new DistributionRunWriter();
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
    public Step masterStepDisRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepDisRun")
                .partitioner(slaveStepDisRun(stepBuilderFactory).getName(), partitionerDisRun())
                .step(slaveStepDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

    @Bean(name="DistributionBatchJob")
    public Job distributionBatchJob(DistributionRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("DistributionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepDisRun(stepBuilderFactory,constants))
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
    public DistributionRunPartitionerYearly partitionerDisRunYearly() {
        return new DistributionRunPartitionerYearly();
    }

    @Bean
    public Step masterStepDisRunYearly(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepDisRunYearly")
                .partitioner(slaveStepDisRun(stepBuilderFactory).getName(), partitionerDisRunYearly())
                .step(slaveStepDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

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
    public Step masterStepUserReqDisRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepUserReqDisRun")
                .partitioner(slaveStepDisRun(stepBuilderFactory).getName(), partitionerDisRunUserReq())
                .step(slaveStepDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }

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
    public Step slaveStepBlankDisRun(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepBlankDisRun")
                .<BlankCredentialDistribution, BlankCredentialDistribution>chunk(1)
                .reader(itemReaderBlankDisRun())
                .processor(itemProcessorBlankDisRun())
                .writer(itemWriterBlankDisRun())
                .build();
    }

    @Bean
    public Step masterStepBlankUserReqDisRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepBlankUserReqDisRun")
                .partitioner(slaveStepBlankDisRun(stepBuilderFactory).getName(), partitionerDisRunBlankUserReq())
                .step(slaveStepBlankDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }


    @Bean(name="blankDistributionBatchJob")
    public Job blankDistributionBatchJobUserReq(UserReqBlankDistributionRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("blankDistributionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepBlankUserReqDisRun(stepBuilderFactory,constants))
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
    public Step slaveStepPsiDisRun(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepPsiDisRun")
                .<PsiCredentialDistribution, PsiCredentialDistribution>chunk(1)
                .reader(itemReaderPsiDisRun())
                .processor(itemProcessorPsiDisRun())
                .writer(itemWriterPsiDisRun())
                .build();
    }

    @Bean
    public Step masterStepPsiUserReqDisRun(StepBuilderFactory stepBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return stepBuilderFactory.get("masterStepPsiUserReqDisRun")
                .partitioner(slaveStepPsiDisRun(stepBuilderFactory).getName(), partitionerDisRunPsiUserReq())
                .step(slaveStepPsiDisRun(stepBuilderFactory))
                .gridSize(constants.getNumberOfPartitions())
                .taskExecutor(taskExecutor(constants))
                .build();
    }


    @Bean(name="psiDistributionBatchJob")
    public Job psiDistributionBatchJobUserReq(UserReqPsiDistributionRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory, EducGradBatchGraduationApiConstants constants) {
        return jobBuilderFactory.get("psiDistributionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepPsiUserReqDisRun(stepBuilderFactory,constants))
                .end()
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
    public Step slaveStepUserScheduled(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepUserScheduled")
                .<UserScheduledJobs, UserScheduledJobs>chunk(1)
                .reader(itemReaderUserScheduled())
                .processor(itemProcessorUserScheduled())
                .writer(itemWriterUserScheduled())
                .build();
    }

    @Bean(name="userScheduledBatchJobRefresher")
    public Job userScheduledBatchJobQueueRefresher(UserScheduledCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("userScheduledBatchJobRefresher")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(slaveStepUserScheduled(stepBuilderFactory))
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
        executor.setThreadNamePrefix("partition_task_executor_thread-");
        executor.initialize();
        return executor;
    }
}