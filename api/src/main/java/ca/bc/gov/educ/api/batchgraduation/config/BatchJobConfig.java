package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.GradRunCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.listener.TvrRunJobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.processor.RegGradAlgPartitionHandlerCreator;
import ca.bc.gov.educ.api.batchgraduation.processor.TvrRunPartitionHandlerCreator;
import ca.bc.gov.educ.api.batchgraduation.reader.RegGradAlgPartitioner;
import ca.bc.gov.educ.api.batchgraduation.reader.TvrRunPartitioner;
import ca.bc.gov.educ.api.batchgraduation.service.GradStudentService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
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
    public Step masterStepRegGrad(StepBuilderFactory stepBuilderFactory, GradStudentService gradStudentService) {
        return stepBuilderFactory.get("masterStepRegGrad")
                .partitioner(slaveStepRegGrad(stepBuilderFactory).getName(), partitionerRegGrad(gradStudentService))
                .step(slaveStepRegGrad(stepBuilderFactory))
                .gridSize(5)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public RegGradAlgPartitioner partitionerRegGrad(GradStudentService gradStudentService) {
        return new RegGradAlgPartitioner(gradStudentService);
    }

    @Bean
    public Step slaveStepRegGrad(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepRegGrad")
                .tasklet(regGradAlgPartitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public RegGradAlgPartitionHandlerCreator regGradAlgPartitionHandler() {
        return new RegGradAlgPartitionHandlerCreator();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="GraduationBatchJob")
    public Job graduationBatchJob(GradRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory, GradStudentService gradStudentService,
                                  JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("GraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepRegGrad(stepBuilderFactory, gradStudentService))
                .end()
                .build();
    }


    // Partitioning for Regular TVR Run updates
    @Bean
    public Step masterStepTvrRun(StepBuilderFactory stepBuilderFactory, GradStudentService gradStudentService) {
        return stepBuilderFactory.get("masterStepTvrRun")
                .partitioner(slaveStepTvrRun(stepBuilderFactory).getName(), partitionerTvrRun(gradStudentService))
                .step(slaveStepTvrRun(stepBuilderFactory))
                .gridSize(5)
                .taskExecutor(taskExecutor())
                .build();
    }

    @Bean
    public TvrRunPartitioner partitionerTvrRun(GradStudentService gradStudentService) {
        return new TvrRunPartitioner(gradStudentService);
    }

    @Bean
    public Step slaveStepTvrRun(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("slaveStepTvrRun")
                .tasklet(tvrRunPartitionHandlerCreator())
                .build();
    }

    @Bean
    @StepScope
    public TvrRunPartitionHandlerCreator tvrRunPartitionHandlerCreator() {
        return new TvrRunPartitionHandlerCreator();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="tvrBatchJob")
    public Job tvrBatchJob(TvrRunJobCompletionNotificationListener listener,StepBuilderFactory stepBuilderFactory, GradStudentService gradStudentService,
                           JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("tvrBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepTvrRun(stepBuilderFactory, gradStudentService))
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
