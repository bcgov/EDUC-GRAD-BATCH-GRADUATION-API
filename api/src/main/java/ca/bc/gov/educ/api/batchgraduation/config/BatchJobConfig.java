package ca.bc.gov.educ.api.batchgraduation.config;

import ca.bc.gov.educ.api.batchgraduation.listener.TvrRunJobCompletionNotificationListener;
import ca.bc.gov.educ.api.batchgraduation.processor.RunProjectedGradAlgorithmProcessor;
import ca.bc.gov.educ.api.batchgraduation.reader.RecalculateProjectedGradRunReader;
import ca.bc.gov.educ.api.batchgraduation.writer.TvrRunBatchPerformanceWriter;
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
    public ItemReader<GraduationStudentRecord> itemReaderRegGrad(RestUtils restUtils) {
        return new RecalculateStudentReader(restUtils);
    }

    @Bean
    public ItemReader<GraduationStudentRecord> itemReaderTvrRun(RestUtils restUtils) {
        return new RecalculateProjectedGradRunReader(restUtils);
    }

    @Bean
    public ItemWriter<GraduationStudentRecord> itemWriterRegGrad() {
        return new BatchPerformanceWriter();
    }

    @Bean
    public ItemWriter<GraduationStudentRecord> itemWriterTvrRun() {
        return new TvrRunBatchPerformanceWriter();
    }
    
    @Bean
	public ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> itemProcessorRegGrad() {
		return new RunGradAlgorithmProcessor();
	}

    @Bean
    public ItemProcessor<GraduationStudentRecord,GraduationStudentRecord> itemProcessorTvrRun() {
        return new RunProjectedGradAlgorithmProcessor();
    }

    /**
     * Creates a bean that represents the only step of our batch job.
     */
    @Bean
    public Step graduationJobStep(ItemReader<GraduationStudentRecord> itemReaderRegGrad,
    						   org.springframework.batch.item.ItemProcessor<? super GraduationStudentRecord, ? extends GraduationStudentRecord> itemProcessorRegGrad,
                               ItemWriter<GraduationStudentRecord> itemWriterRegGrad,
                               StepBuilderFactory stepBuilderFactory) {
        return stepBuilderFactory.get("graduationJobStep")
                .<GraduationStudentRecord, GraduationStudentRecord>chunk(1)
                .reader(itemReaderRegGrad)
                .processor(itemProcessorRegGrad)
                .writer(itemWriterRegGrad)
                .build();
    }

    /**
     * Creates a bean that represents our batch job.
     */
    @Bean(name="GraduationBatchJob")
    public Job graduationBatchJob(Step graduationJobStep,JobCompletionNotificationListener listener,
                          JobBuilderFactory jobBuilderFactory) {
        return jobBuilderFactory.get("GraduationBatchJob")
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(graduationJobStep)               
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
}
