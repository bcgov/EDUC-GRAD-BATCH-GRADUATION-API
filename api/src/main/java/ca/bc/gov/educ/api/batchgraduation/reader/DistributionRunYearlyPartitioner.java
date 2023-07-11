package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
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

public class DistributionRunYearlyPartitioner extends BasePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Autowired
    RestUtils restUtils;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        // Clean up existing reports before running new one
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_YE", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "DISTREP_YE_SC", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "DISTREP_YE_SD", restUtils.getAccessToken());

        List<StudentCredentialDistribution> credentialList = parallelDataFetch.fetchStudentCredentialsDistributionDataYearly();
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
