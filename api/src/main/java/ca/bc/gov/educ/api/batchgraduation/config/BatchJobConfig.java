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

import ca.bc.gov.educ.api.batchgraduation.listener.JobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.processor.RunGradAlgorithmProcessor;
import ca.bc.gov.educ.api.batchgraduation.reader.RecalculateStudentReader;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.writer.BatchPerformanceWriter;

@Configuration
public class BatchJobConfig {

	@Autowired
	JobRegistry jobRegistry;
	  
    @Bean
    public ItemReader<GraduationStudentRecord> itemReader(RestUtils restUtils) {
        return new RecalculateStudentReader(restUtils);
    }

    @Bean
    public ItemWriter<GraduationStudentRecord> itemWriter() {
        return new BatchPerformanceWriter();
    }
    
    @Bean
	public ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> itemProcessor() {
		return new RunGradAlgorithmProcessor();
	}

    /**
     * Creates a bean that represents the only step of our batch job.
     */
    @Bean
    public Step graduationJobStep(ItemReader<GraduationStudentRecord> itemReader,
    						   org.springframework.batch.item.ItemProcessor<? super GraduationStudentRecord, ? extends GraduationStudentRecord> itemProcessor,
                               ItemWriter<GraduationStudentRecord> itemWriter,
                               StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("graduationJobStep")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
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
