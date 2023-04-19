package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.BatchGraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public abstract class BaseProcessor implements ItemProcessor<UUID, GraduationStudentRecord> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseProcessor.class);

    @Autowired
    RestUtils restUtils;

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution.id}")
    Long batchId;

    protected GraduationStudentRecord getItem(UUID key) {
        if (key == null)
            return null;
        BatchGraduationStudentRecord item = restUtils.getStudentForBatchInput(key, summaryDTO);
        if (item != null) {
            GraduationStudentRecord inputRecord = new GraduationStudentRecord();
            BeanUtils.copyProperties(item, inputRecord);
            // update input data
            gradBatchHistoryService.saveBatchAlgorithmStudent(batchId, inputRecord.getStudentID(), inputRecord.getProgram(), inputRecord.getSchoolOfRecord());
            return inputRecord;
        }
        return null;
    }
}
