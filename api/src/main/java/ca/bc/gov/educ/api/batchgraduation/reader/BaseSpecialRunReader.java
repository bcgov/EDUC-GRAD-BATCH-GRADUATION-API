package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

public abstract class BaseSpecialRunReader implements ItemReader<GraduationStudentRecord> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSpecialRunReader.class);

    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['index']}")
    protected Integer nxtStudentForProcessing;

    @Value("#{stepExecutionContext['data']}")
    protected List<GraduationStudentRecord> studentList;

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    protected void aggregate(String summaryContextName) {
        AlgorithmSummaryDTO totalSummaryDTO = (AlgorithmSummaryDTO)jobExecution.getExecutionContext().get(summaryContextName);
        totalSummaryDTO.setReadCount(totalSummaryDTO.getReadCount() + summaryDTO.getReadCount());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.getErrors().putAll(summaryDTO.getErrors());
        totalSummaryDTO.getSuccessfulStudentIDs().addAll(summaryDTO.getSuccessfulStudentIDs());
        totalSummaryDTO.getSchoolList().addAll(summaryDTO.getSchoolList());
        mergeMapCounts(totalSummaryDTO.getProgramCountMap(),summaryDTO.getProgramCountMap());
    }

    protected void fetchAccessToken() {
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
        }
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