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
import java.util.UUID;

public class DistributionRunYearlyPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(DistributionRunYearlyPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        // Clean up existing reports before running new one
        logger.debug("Delete School Reports for Yearly Distribution");
        long startTime = System.currentTimeMillis();
        restUtils.deleteSchoolReportRecord("ADDRESS_LABEL_SCHL");
        restUtils.deleteDistrictReportRecord("ADDRESS_LABEL_YE");
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Old School Reports deleted in {} sec", diff);

        startTime = System.currentTimeMillis();
        logger.debug("Retrieve students for Yearly Distribution");
        List<StudentCredentialDistribution> eligibleStudentSchoolDistricts = parallelDataFetch.fetchStudentCredentialsDistributionDataYearly();
        endTime = System.currentTimeMillis();
        diff = (endTime - startTime)/1000;
        logger.debug("Total {} eligible StudentCredentialDistributions found in {} sec", eligibleStudentSchoolDistricts.size(), diff);
        filterByStudentSearchRequest(eligibleStudentSchoolDistricts);
        if(!eligibleStudentSchoolDistricts.isEmpty()) {
            updateBatchJobHistory(createBatchJobHistory(), (long) eligibleStudentSchoolDistricts.size());
            List<UUID> schoolIds = eligibleStudentSchoolDistricts.stream().map(StudentCredentialDistribution::getSchoolId).distinct().toList();
            for(UUID schoolId: schoolIds) {
                restUtils.deleteSchoolReportRecord(schoolId, "DISTREP_YE_SC");
            }
            List<UUID> districtIds = eligibleStudentSchoolDistricts.stream().map(StudentCredentialDistribution::getDistrictId).distinct().toList();
            for(UUID districtId: districtIds) {
                restUtils.deleteDistrictReportRecord(districtId, "DISTREP_YE_SD");
            }
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
