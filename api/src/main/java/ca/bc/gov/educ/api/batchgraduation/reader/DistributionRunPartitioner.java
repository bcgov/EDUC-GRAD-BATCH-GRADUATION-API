package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionDataParallelDTO;
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

/**
 * Monthly Distribution Partitioner
 */
public class DistributionRunPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(DistributionRunPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        // Clean up existing reports before running new one
        logger.debug("Delete School Reports for Monthly Distribution");
        long startTime = System.currentTimeMillis();
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL");
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Old School Reports deleted in {} sec", diff);

        startTime = System.currentTimeMillis();
        logger.debug("Retrieve students for Monthly Distribution");
        Mono<DistributionDataParallelDTO> parallelDTOMono = parallelDataFetch.fetchDistributionRequiredData();
        DistributionDataParallelDTO parallelDTO = parallelDTOMono.block();
        List<StudentCredentialDistribution> credentialList = new ArrayList<>();
        if(parallelDTO != null) {
            credentialList.addAll(parallelDTO.transcriptList());
            credentialList.addAll(parallelDTO.certificateList());
        }
        endTime = System.currentTimeMillis();
        diff = (endTime - startTime)/1000;
        logger.debug("Total {} eligible StudentCredentialDistributions found in {} sec", credentialList.size(), diff);
        filterByStudentSearchRequest(credentialList);
        if(!credentialList.isEmpty()) {
            filterOutDeceasedStudents(credentialList);
            updateBatchJobHistory(createBatchJobHistory(), (long) credentialList.size());
            return getStringExecutionContextMap(gridSize, credentialList, null);
        }
        logger.info("No Credentials Found for Processing");
        return new HashMap<>();
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
