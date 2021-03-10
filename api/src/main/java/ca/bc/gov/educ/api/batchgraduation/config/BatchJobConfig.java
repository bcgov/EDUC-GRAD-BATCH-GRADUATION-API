package ca.bc.gov.educ.api.batchgraduation.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.batchgraduation.listener.JobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.processor.RunGradAlgorithmProcessor;
import ca.bc.gov.educ.api.batchgraduation.reader.RecalculateStudentReader;
import ca.bc.gov.educ.api.batchgraduation.writer.BatchPerformanceWriter;


@Configuration
public class BatchJobConfig {

	private static final String ENDPOINT_STUDENT_FOR_GRADUATION_LIST_URL = "endpoint.grad-graduation-status-api.student-for-grad-list.url";

	@Autowired
	JobRegistry jobRegistry;
	  
    @Bean
    public ItemReader<GraduationStatus> itemReader(Environment environment, RestTemplate restTemplate) {
        return new RecalculateStudentReader(environment.getRequiredProperty(ENDPOINT_STUDENT_FOR_GRADUATION_LIST_URL), restTemplate);
    }

    @Bean
    public ItemWriter<GraduationStatus> itemWriter() {
        return new BatchPerformanceWriter();
    }
    
    @Bean
	public ItemProcessor<GraduationStatus,GraduationStatus> itemProcessor() {
		return new RunGradAlgorithmProcessor();
	}

    /**
     * Creates a bean that represents the only step of our batch job.
     * @param reader
     * @param writer
     * @param stepBuilderFactory
     * @return
     */
    @Bean
    public Step graduationJobStep(ItemReader<GraduationStatus> reader,
    						   org.springframework.batch.item.ItemProcessor<? super GraduationStatus, ? extends GraduationStatus> processor,
                               ItemWriter<GraduationStatus> writer,
                               StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("graduationJobStep")
                .<GraduationStatus, GraduationStatus>chunk(1)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     * @param graduationJobStep
     * @param jobBuilderFactory
     * @param listener
     * @return
     */
    @Bean
    public Job graduationBatchJob(Step graduationJobStep,JobCompletionNotificationListener listener,
                          JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("GraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(graduationJobStep)               
                .end()
                .build();
    }

    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor() {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }
}
