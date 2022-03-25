package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.GradRunCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.listener.TvrRunJobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.processor.RunProjectedGradAlgorithmProcessor;
import ca.bc.gov.educ.api.batchgraduation.processor.RunRegularGradAlgorithmProcessor;
import ca.bc.gov.educ.api.batchgraduation.reader.RecalculateProjectedGradRunReader;
import ca.bc.gov.educ.api.batchgraduation.reader.RecalculateStudentReader;
import ca.bc.gov.educ.api.batchgraduation.reader.RegGradAlgPartitioner;
import ca.bc.gov.educ.api.batchgraduation.reader.TvrRunPartitioner;
import ca.bc.gov.educ.api.batchgraduation.writer.RegGradAlgBatchPerformanceWriter;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchJobConfig {

	@Autowired
	JobRegistry jobRegistry;

    // Partitioning for Regular Grad Run updates

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
    public ItemWriter<GraduationStudentRecord> itemWriterRegGrad() {
        return new RegGradAlgBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepRegGrad(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("masterStepRegGrad")
                .partitioner(graduationJobStep(stepBuilderFactory).getName(), partitionerRegGrad())
                .step(graduationJobStep(stepBuilderFactory))
                .gridSize(5)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public RegGradAlgPartitioner partitionerRegGrad() {
        return new RegGradAlgPartitioner();
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
    public Job graduationBatchJob(GradRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory,JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("GraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepRegGrad(stepBuilderFactory))
                .end()
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
    public ItemWriter<GraduationStudentRecord> itemWriterTvrRun() {
        return new TvrRunBatchPerformanceWriter();
    }

    @Bean
    public Step masterStepTvrRun(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("masterStepTvrRun")
                .partitioner(tvrJobStep(stepBuilderFactory).getName(), partitionerTvrRun())
                .step(tvrJobStep(stepBuilderFactory))
                .gridSize(5)
                .taskExecutor(taskExecutor())
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

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="tvrBatchJob")
    public Job tvrBatchJob(TvrRunJobCompletionNotificationListener listener,StepBuilderFactory stepBuilderFactory,JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("tvrBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepTvrRun(stepBuilderFactory))
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
        executor.setCorePoolSize(15);
        executor.setMaxPoolSize(15);
        executor.setThreadNamePrefix("partition_task_executor_thread-");
        executor.initialize();
        return executor;
    }
}
