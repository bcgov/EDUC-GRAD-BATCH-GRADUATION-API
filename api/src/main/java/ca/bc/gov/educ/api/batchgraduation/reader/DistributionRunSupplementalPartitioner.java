package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // Clean up existing reports before running new one
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "DISTREP_SC", restUtils.getAccessToken());
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Old School Reports deleted in {} sec", diff);

        startTime = System.currentTimeMillis();
        logger.debug("Retrieve students for Supplemental Distribution");
        List<StudentCredentialDistribution> eligibleStudentSchoolDistricts = parallelDataFetch.fetchStudentCredentialsDistributionDataYearly();
        endTime = System.currentTimeMillis();
        diff = (endTime - startTime)/1000;
        logger.debug("Total {} eligible StudentCredentialDistributions found in {} sec", eligibleStudentSchoolDistricts.size(), diff);
        filterByStudentSearchRequest(eligibleStudentSchoolDistricts);
        if(!eligibleStudentSchoolDistricts.isEmpty()) {
            updateBatchJobHistory(createBatchJobHistory(), (long) eligibleStudentSchoolDistricts.size());
            return getStringExecutionContextMap(gridSize, eligibleStudentSchoolDistricts, null);
        }
        logger.info("No Credentials Found for Processing");
        return new HashMap<>();
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
