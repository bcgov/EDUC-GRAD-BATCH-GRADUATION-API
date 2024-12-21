package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public abstract class BaseMinCodeReader implements ItemReader<String> {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['index']}")
    Integer nextSchoolForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<String> schools;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

}