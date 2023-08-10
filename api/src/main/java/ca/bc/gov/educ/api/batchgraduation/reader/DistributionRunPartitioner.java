package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionDataParallelDTO;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistributionRunPartitioner extends BasePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        ResponseObj res = restUtils.getTokenResponseObject();
        String accessToken = null;
        if (res != null) {
            accessToken = res.getAccess_token();
        }

        // Clean up existing reports before running new one
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL", restUtils.getAccessToken());

        Mono<DistributionDataParallelDTO> parallelDTOMono = parallelDataFetch.fetchDistributionRequiredData(accessToken);
        DistributionDataParallelDTO parallelDTO = parallelDTOMono.block();
        List<StudentCredentialDistribution> credentialList = new ArrayList<>();
        if(parallelDTO != null) {
            credentialList.addAll(parallelDTO.transcriptList());
            credentialList.addAll(parallelDTO.certificateList());
        }
        if(!credentialList.isEmpty()) {
            updateBatchJobHistory(createBatchJobHistory(), (long) credentialList.size());
            return getStringExecutionContextMap(gridSize, credentialList, null, LOGGER);
        }
        LOGGER.info("No Credentials Found for Processing");
        return new HashMap<>();
    }


    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
