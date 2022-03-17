package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

public abstract class BasePartitionHandlerCreator implements Tasklet {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasePartitionHandlerCreator.class);

    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['data']}")
    List<GraduationStudentRecord> partitionData;

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution.jobId}")
    Long batchId;

    protected void aggregate(StepContribution contribution) {
        AlgorithmSummaryDTO totalSummaryDTO = (AlgorithmSummaryDTO)contribution.getStepExecution().getJobExecution().getExecutionContext().get("tvrRunSummaryDTO");
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new AlgorithmSummaryDTO();
            contribution.getStepExecution().getJobExecution().getExecutionContext().put("tvrRunSummaryDTO", totalSummaryDTO);
        }
        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.getErrors().addAll(summaryDTO.getErrors());
        mergeMapCounts(totalSummaryDTO.getProgramCountMap(),summaryDTO.getProgramCountMap());
    }

    protected String fetchAccessToken() {
        LOGGER.info("Fetching the access token from KeyCloak API");
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            return res.getAccess_token();
        }
        return null;
    }

    private void mergeMapCounts(Map<String, Long> total, Map<String, Long> current) {
        current.forEach((k,v) -> {
            if (total.containsKey(k)) {
                total.put(k, total.get(k) + v);
            } else {
                total.put(k, v);
            }
        });
    }
}