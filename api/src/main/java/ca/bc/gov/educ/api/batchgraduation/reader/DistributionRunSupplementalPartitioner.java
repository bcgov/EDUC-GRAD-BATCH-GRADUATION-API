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

import java.util.*;

public class DistributionRunSupplementalPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(DistributionRunSupplementalPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        logger.debug("Delete School Reports for Supplemental Distribution");
        long startTime = System.currentTimeMillis();
        String accessToken = restUtils.getAccessToken();
        // Clean up existing reports before running new one
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL", accessToken);
        restUtils.deleteSchoolReportRecord("", "DISTREP_SC", accessToken);
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Old School Reports deleted in {} sec", diff);

        startTime = System.currentTimeMillis();
        logger.debug("Retrieve students for Supplemental Distribution");
        Mono<DistributionDataParallelDTO> parallelDTOMono = parallelDataFetch.fetchDistributionRequiredDataYearly(accessToken);
        DistributionDataParallelDTO parallelDTO = parallelDTOMono.block();
        List<StudentCredentialDistribution> eligibleStudentSchoolDistricts = new ArrayList<>();
        if(parallelDTO != null) {
            eligibleStudentSchoolDistricts.addAll(parallelDTO.transcriptList());
            eligibleStudentSchoolDistricts.addAll(parallelDTO.certificateList());
        }
        endTime = System.currentTimeMillis();
        diff = (endTime - startTime)/1000;
        logger.debug("Total {} eligible StudentCredentialDistributions found in {} sec", eligibleStudentSchoolDistricts.size(), diff);
        filterByStudentSearchRequest(eligibleStudentSchoolDistricts);
        if(!eligibleStudentSchoolDistricts.isEmpty()) {
            filterOutDeceasedStudents(eligibleStudentSchoolDistricts);
            updateBatchJobHistory(createBatchJobHistory(), (long) eligibleStudentSchoolDistricts.size());
            return getStringExecutionContextMap(gridSize, eligibleStudentSchoolDistricts, null, logger);
        }
        logger.info("No Credentials Found for Processing");
        return new HashMap<>();
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
