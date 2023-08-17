package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistributionRunPartitionerUserReq extends BasePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunPartitionerUserReq.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }
        JobParameters jobParameters = context.getJobParameters();
        String credentialType = jobParameters.getString("credentialType");
        StudentSearchRequest req = getStudentSearchRequest();
        List<StudentCredentialDistribution> credentialList = restUtils.getStudentsForUserReqDisRun(credentialType,req,accessToken);
        if(!credentialList.isEmpty()) {
            Map<String, ExecutionContext> map = getStringExecutionContextMap(gridSize, credentialList, credentialType, LOGGER);
            LOGGER.info("Found {} in total running on {} partitions",credentialList.size(),map.size());
            return map;
        }
        LOGGER.info("No Credentials Found for Processing");
        return new HashMap<>();
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
