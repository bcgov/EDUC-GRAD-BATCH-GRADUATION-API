package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.DataConversionJobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.processor.DataConversionProcessor;
import ca.bc.gov.educ.api.batchgraduation.reader.DataConversionStudentReader;
import ca.bc.gov.educ.api.batchgraduation.service.DataConversionService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.writer.DataConversionStudentWriter;
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
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.batchgraduation.listener.JobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import ca.bc.gov.educ.api.batchgraduation.processor.RunGradAlgorithmProcessor;
import ca.bc.gov.educ.api.batchgraduation.reader.RecalculateStudentReader;
import ca.bc.gov.educ.api.batchgraduation.writer.BatchPerformanceWriter;

@Configuration
public class BatchJobConfig {

	@Autowired
	JobRegistry jobRegistry;
	  
    @Bean
    public ItemReader<GraduationStatus> itemReader(EducGradBatchGraduationApiConstants constants, RestTemplate restTemplate) {
        return new RecalculateStudentReader(restTemplate, constants);
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
     */
    @Bean
    public Step graduationJobStep(ItemReader<GraduationStatus> itemReader,
    						   org.springframework.batch.item.ItemProcessor<? super GraduationStatus, ? extends GraduationStatus> itemProcessor,
                               ItemWriter<GraduationStatus> itemWriter,
                               StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("graduationJobStep")
                .<GraduationStatus, GraduationStatus>chunk(1)
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
  public ItemReader<ConvGradStudent> dataConversionReader(DataConversionService dataConversionService, RestUtils restUtils) {
    return new DataConversionStudentReader(dataConversionService, restUtils);
  }

  @Bean
  public ItemWriter<ConvGradStudent> dataConversionWriter() {
    return new DataConversionStudentWriter();
  }

  @Bean
  public ItemProcessor<ConvGradStudent,ConvGradStudent> dataConversionProcessor() {
    return new DataConversionProcessor();
  }

  /**
   * Creates a bean that represents the only step of our batch job.
   */
  @Bean
  public Step dataConversionJobStep(ItemReader<ConvGradStudent> dataConversionReader,
                                    ItemProcessor<? super ConvGradStudent, ? extends ConvGradStudent> dataConversionProcessor,
                                    ItemWriter<ConvGradStudent> dataConversionWriter,
                                    StepBuilderFactory stepBuilderFactory) {
    return stepBuilderFactory.get("dataConversionJobStep")
            .<ConvGradStudent, ConvGradStudent>chunk(1)
            .reader(dataConversionReader)
            .processor(dataConversionProcessor)
            .writer(dataConversionWriter)
            .build();
  }

  /**
   * Creates a bean that represents our batch job.
   */
  @Bean(name="dataConversionJob")
  public Job dataConversionBatchJob(Step dataConversionJobStep, DataConversionJobCompletionNotificationListener listener,
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
