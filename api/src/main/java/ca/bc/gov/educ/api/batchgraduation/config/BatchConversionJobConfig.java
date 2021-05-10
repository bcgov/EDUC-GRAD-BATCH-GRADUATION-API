package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.JobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.processor.RunGradAlgorithmProcessor;
import ca.bc.gov.educ.api.batchgraduation.reader.DataConversionStudentReader;
import ca.bc.gov.educ.api.batchgraduation.reader.RecalculateStudentReader;
import ca.bc.gov.educ.api.batchgraduation.writer.BatchPerformanceWriter;
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


//@Configuration
public class BatchConversionJobConfig {

	private static final String ENDPOINT_STUDENT_FOR_GRADUATION_LIST_URL = "endpoint.grad-graduation-status-api.student-for-grad-list.url";

	@Autowired
	JobRegistry jobRegistry;
	  
    @Bean
    public ItemReader<ConvGradStudent> dataReader() {
        return new DataConversionStudentReader();
    }
//
//    @Bean
//    public ItemWriter<ConvGradStudent> dataWriter() {
//
//      return new BatchPerformanceWriter();
//    }
//
//    @Bean
//    public ItemProcessor<ConvGradStudent,ConvGradStudent> itemProcessor() {
//      return new RunGradAlgorithmProcessor();
//    }

    /**
     * Creates a bean that represents the only step of our batch job.
     * @param reader
     * @param writer
     * @param stepBuilderFactory
     * @return
     */
    @Bean
    public Step dataConversionJobStep(ItemReader<ConvGradStudent> reader,
    						   ItemProcessor<? super ConvGradStudent, ? extends ConvGradStudent> processor,
                               ItemWriter<ConvGradStudent> writer,
                               StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("dataConversionJobStep")
                .<ConvGradStudent, ConvGradStudent>chunk(1)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     * @param dataConversionJobStep
     * @param jobBuilderFactory
     * @param listener
     * @return
     */
    @Bean
    public Job dataConversionBatchJob(Step dataConversionJobStep,JobCompletionNotificationListener listener,
                          JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("dataConversionBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(dataConversionJobStep)
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
