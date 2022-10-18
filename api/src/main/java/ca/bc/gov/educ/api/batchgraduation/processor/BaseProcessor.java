package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.BatchGraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public abstract class BaseProcessor implements ItemProcessor<UUID, GraduationStudentRecord> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseProcessor.class);

    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution.id}")
    Long batchId;

    protected GraduationStudentRecord getItem(UUID key) {
        BatchGraduationStudentRecord item = restUtils.getStudentForBatchInput(key, summaryDTO);
        if (item != null) {
            GraduationStudentRecord inputRecord = new GraduationStudentRecord();
            BeanUtils.copyProperties(item, inputRecord);
            return inputRecord;
        }
        return null;
    }
}
