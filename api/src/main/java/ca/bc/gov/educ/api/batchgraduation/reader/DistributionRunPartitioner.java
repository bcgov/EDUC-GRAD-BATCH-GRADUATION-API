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

import java.util.*;

/**
 * Monthly Distribution Partitioner
 */
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
        LOGGER.debug("Delete School Reports for Monthly Distribution");
        long startTime = System.currentTimeMillis();
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL", restUtils.getAccessToken());
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        LOGGER.debug("Old School Reports deleted in {} sec", diff);

        startTime = System.currentTimeMillis();
        LOGGER.debug("Retrieve students for Monthly Distribution");
        Mono<DistributionDataParallelDTO> parallelDTOMono = parallelDataFetch.fetchDistributionRequiredData(accessToken);
        DistributionDataParallelDTO parallelDTO = parallelDTOMono.block();
        List<StudentCredentialDistribution> credentialList = new ArrayList<>();
        if(parallelDTO != null) {
            credentialList.addAll(parallelDTO.transcriptList());
            credentialList.addAll(parallelDTO.certificateList());
        }
        endTime = System.currentTimeMillis();
        diff = (endTime - startTime)/1000;
        LOGGER.debug("Total {} eligible StudentCredentialDistributions found in {} sec", credentialList.size(), diff);
        if(!credentialList.isEmpty()) {
            LOGGER.debug("Total size of credential list: {}", credentialList.size());
            // Filter deceased students out
            List<UUID> deceasedIDs = restUtils.getDeceasedStudentIDs(credentialList.stream().map(StudentCredentialDistribution::getStudentID).distinct().toList(), restUtils.getAccessToken());
            if (!deceasedIDs.isEmpty()) {
                LOGGER.debug("Deceased students: {}", deceasedIDs.size());
                credentialList.removeIf(cr -> deceasedIDs.contains(cr.getStudentID()));
                LOGGER.debug("Revised size of credential list: {}", credentialList.size());
            }
            updateBatchJobHistory(createBatchJobHistory(), (long) credentialList.size());
            return getStringExecutionContextMap(gridSize, credentialList, null);
        }
        LOGGER.info("No Credentials Found for Processing");
        return new HashMap<>();
    }


    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
