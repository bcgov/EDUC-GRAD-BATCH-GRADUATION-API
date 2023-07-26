package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.School;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistributionRunYearlyPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(DistributionRunYearlyPartitioner.class);

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

        logger.debug("Retrieve students for early distribution");
        List<StudentCredentialDistribution> eligibleStudentSchoolDistricts = parallelDataFetch.fetchStudentCredentialsDistributionDataYearly();
        logger.debug("Total {} eligible StudentCredentialDistributions found", eligibleStudentSchoolDistricts);
        StudentSearchRequest searchRequest = getStudentSearchRequest();
        if(searchRequest != null && searchRequest.getSchoolCategoryCodes() != null && !searchRequest.getSchoolCategoryCodes().isEmpty()) {
            List<String> useFilterSchoolDistricts = new ArrayList<>();
            for(String schoolCategoryCode: searchRequest.getSchoolCategoryCodes()) {
                logger.debug("Use schoolCategory code {} to find list of schools", schoolCategoryCode);
                List<School> schools = restUtils.getSchoolBySchoolCategoryCode(schoolCategoryCode);
                for(School school: schools) {
                    logger.debug("School {} found by schoolCategory code {}", school.getMincode(), schoolCategoryCode);
                    useFilterSchoolDistricts.add(school.getMincode());
                }
            }
            eligibleStudentSchoolDistricts.removeIf(scr->!useFilterSchoolDistricts.contains(scr.getSchoolOfRecord()));
        }
        if(searchRequest != null && searchRequest.getDistricts() != null && !searchRequest.getDistricts().isEmpty()) {
            eligibleStudentSchoolDistricts.removeIf(scr->!searchRequest.getDistricts().contains(StringUtils.substring(scr.getSchoolOfRecord(), 0, 3)));
        }
        if(searchRequest != null && searchRequest.getSchoolOfRecords() != null && !searchRequest.getSchoolOfRecords().isEmpty()) {
            eligibleStudentSchoolDistricts.removeIf(scr->!searchRequest.getSchoolOfRecords().contains(scr.getSchoolOfRecord()));
        }

        if(!eligibleStudentSchoolDistricts.isEmpty()) {
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
