package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.TvrRunJobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.processor.RegGradAlgPartitionHandlerCreator;
import ca.bc.gov.educ.api.batchgraduation.processor.RunProjectedGradAlgorithmProcessor;
import ca.bc.gov.educ.api.batchgraduation.reader.RecalculateProjectedGradRunReader;
import ca.bc.gov.educ.api.batchgraduation.reader.RegGradAlgPartitioner;
import ca.bc.gov.educ.api.batchgraduation.service.GradStudentService;
import ca.bc.gov.educ.api.batchgraduation.writer.TvrRunBatchPerformanceWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
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

import ca.bc.gov.educ.api.batchgraduation.listener.GradRunCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchJobConfig {

	@Autowired
	JobRegistry jobRegistry;

    @Bean
    public ItemReader<GraduationStudentRecord> itemReaderTvrRun(RestUtils restUtils) {
        return new RecalculateProjectedGradRunReader(restUtils);
    }

    @Bean
    public ItemWriter<GraduationStudentRecord> itemWriterTvrRun() {
        return new TvrRunBatchPerformanceWriter();
    }

    @Bean
    public ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> itemProcessorTvrRun() {
        return new RunProjectedGradAlgorithmProcessor();
    }

    // Partitioning for pen updates
    @Bean
    public Step masterStep(StepBuilderFactory stepBuilderFactory, GradStudentService gradStudentService) {
        return stepBuilderFactory.get("masterStep")
                .partitioner(slaveStep(stepBuilderFactory).getName(), partitioner(gradStudentService))
                .step(slaveStep(stepBuilderFactory))
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public RegGradAlgPartitioner partitioner(GradStudentService gradStudentService) {
        // Reader to feed input data for each partition
        return new RegGradAlgPartitioner(gradStudentService);
    }

    @Bean
    public Step slaveStep(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStep")
                .tasklet(regGradAlgPartitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public RegGradAlgPartitionHandlerCreator regGradAlgPartitionHandler() {
        // Processor for each partition
        return new RegGradAlgPartitionHandlerCreator();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="GraduationBatchJob")
    public Job graduationBatchJob(Step masterStep, GradRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, GradStudentService gradStudentService,
                                  JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("GraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStep(stepBuilderFactory, gradStudentService))
                .end()
                .build();
    }

    /**
     * Creates a bean that represents the only step of our batch job.
     */
    @Bean
    public Step tvrJobStep(ItemReader<GraduationStudentRecord> itemReaderTvrRun,
                                  org.springframework.batch.item.ItemProcessor<? super GraduationStudentRecord, ? extends GraduationStudentRecord> itemProcessorTvrRun,
                                  ItemWriter<GraduationStudentRecord> itemWriterTvrRun,
                                  StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("tvrJobStep")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReaderTvrRun)
                .processor(itemProcessorTvrRun)
                .writer(itemWriterTvrRun)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="tvrBatchJob")
    public Job tvrBatchJob(Step tvrJobStep, TvrRunJobCompletionNotificationListener listener,
                           JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("tvrBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(tvrJobStep)
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
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setThreadNamePrefix("partition_task_executor_thread-");
        executor.initialize();
        return executor;
    }
}
