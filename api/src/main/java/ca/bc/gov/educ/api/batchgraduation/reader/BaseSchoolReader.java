package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

public abstract class BaseSchoolReader implements ItemReader<UUID> {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['index']}")
    Integer nextSchoolForProcessing;

    @Value("#{stepExecutionContext['data']}")
    List<UUID> schools;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

}