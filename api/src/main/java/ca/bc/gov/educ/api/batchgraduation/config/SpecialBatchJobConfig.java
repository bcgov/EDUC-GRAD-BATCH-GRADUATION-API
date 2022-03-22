package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.GradRunCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.processor.SpcRegGradAlgPartitionHandlerCreator;
import ca.bc.gov.educ.api.batchgraduation.reader.SpcRegGradAlgPartitioner;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SpecialBatchJobConfig {

	@Autowired
	JobRegistry jobRegistry;

    // Partitioning for Regular Grad Run updates
    @Bean
    public Step masterStepSpcRegGrad(StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("masterStepSpcRegGrad")
                .partitioner(slaveStepSpcRegGrad(stepBuilderFactory).getName(), partitionerSpcRegGrad())
                .step(slaveStepSpcRegGrad(stepBuilderFactory))
                .gridSize(5)
                .taskExecutor(taskExecutorSpc())
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
                .tasklet(spcRegGradAlgPartitionHandler())
                .build();
    }

    @Bean
    @StepScope
    public SpcRegGradAlgPartitionHandlerCreator spcRegGradAlgPartitionHandler() {
        return new SpcRegGradAlgPartitionHandlerCreator();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="SpecialGraduationBatchJob")
    public Job specialGraduationBatchJob(GradRunCompletionNotificationListener listener, StepBuilderFactory stepBuilderFactory,JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("SpecialGraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(masterStepSpcRegGrad(stepBuilderFactory))
                .end()
                .build();
    }

    @Bean
    public TaskExecutor taskExecutorSpc() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setThreadNamePrefix("partition_task_executor_thread-");
        executor.initialize();
        return executor;
    }
}
