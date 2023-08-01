package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.School;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

public class DistributionRunYearlyNonGradPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(DistributionRunYearlyNonGradPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    RestUtils restUtils;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        // Clean up existing reports before running new one
        logger.debug("Delete School Reports for Yearly Distribution");
        long startTime = System.currentTimeMillis();
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_YE", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "NONGRADDISTREP_SC", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "NONGRADDISTREP_SD", restUtils.getAccessToken());
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Old School Reports deleted in {} sec", diff);

        startTime = System.currentTimeMillis();
        logger.debug("Retrieve schools for Non Grad Yearly Distribution");
        List<String> eligibleStudentSchoolDistricts = new ArrayList();
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
            eligibleStudentSchoolDistricts = useFilterSchoolDistricts;
        }
        if(searchRequest != null && searchRequest.getDistricts() != null && !searchRequest.getDistricts().isEmpty()) {
            eligibleStudentSchoolDistricts = searchRequest.getDistricts();
        }
        if(searchRequest != null && searchRequest.getSchoolOfRecords() != null && !searchRequest.getSchoolOfRecords().isEmpty()) {
            eligibleStudentSchoolDistricts = searchRequest.getSchoolOfRecords();
        }
        endTime = System.currentTimeMillis();
        diff = (endTime - startTime)/1000;
        logger.debug("Total {} schools after filters in {} sec", eligibleStudentSchoolDistricts.size(), diff);
        if(eligibleStudentSchoolDistricts.isEmpty()) {
            logger.debug("No filter found, retrieve all districts");
            startTime = System.currentTimeMillis();
            eligibleStudentSchoolDistricts = parallelDataFetch.fetchDistributionRequiredDataDistrictsNonGradYearly(restUtils.getAccessToken());
            endTime = System.currentTimeMillis();
            diff = (endTime - startTime)/1000;
            logger.debug("All {} districts retrieved in {} sec", eligibleStudentSchoolDistricts.size(), diff);
        }
        List<String> finalSchoolDistricts = eligibleStudentSchoolDistricts.stream().sorted().toList();
        if(logger.isDebugEnabled()) {
            logger.debug("Final list of eligible District / School codes {}", String.join(", ", finalSchoolDistricts));
        }
        if(!finalSchoolDistricts.isEmpty()) {
            updateBatchJobHistory(createBatchJobHistory(), (long) finalSchoolDistricts.size());
            int partitionSize = finalSchoolDistricts.size()/gridSize + 1;
            List<List<String>> partitions = new LinkedList<>();
            for (int i = 0; i < finalSchoolDistricts.size(); i += partitionSize) {
                partitions.add(finalSchoolDistricts.subList(i, Math.min(i + partitionSize, finalSchoolDistricts.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
                summaryDTO.initializeCredentialCountMap();
                summaryDTO.setCredentialType("NONGRADYERUN");
                List<String> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                executionContext.put("index",0);
                executionContext.put("searchRequestObject", searchRequest);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            logger.info("Found {} in total running on {} partitions",finalSchoolDistricts.size(),map.size());
            return map;
        }
        logger.info("No Credentials Found for Processing");
        return new HashMap<>();
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
