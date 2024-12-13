package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

public abstract class BaseStudentReader implements ItemReader<UUID> {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    RestUtils restUtils;

    @Value("#{stepExecutionContext['index']}")
    protected Integer nxtStudentForProcessing;

    @Value("#{stepExecutionContext['runBy']}")
    protected String runBy;

    @Value("#{stepExecutionContext['data']}")
    protected List<UUID> studentList;

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    protected void fetchAccessToken() {
        ResponseObj res = restUtils.getTokenResponseObject();
        if (res != null) {
            summaryDTO.setAccessToken(res.getAccess_token());
        }
    }

    protected void setUserName() {
        if (StringUtils.isNotBlank(runBy) && StringUtils.isBlank(ThreadLocalStateUtil.getCurrentUser())) {
            ThreadLocalStateUtil.setCurrentUser(runBy);
        }
    }
}