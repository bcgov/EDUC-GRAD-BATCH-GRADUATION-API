package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import ca.bc.gov.educ.api.batchgraduation.util.GradSchoolOfRecordFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DistributionRunYearlyNonGradPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(DistributionRunYearlyNonGradPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    ParallelDataFetch parallelDataFetch;
    @Autowired
    GradSchoolOfRecordFilter gradSchoolOfRecordFilter;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        // Clean up existing reports before running new one
        logger.debug("Delete School Reports for Yearly Distribution");
        long startTime = System.currentTimeMillis();
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL");
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_YE");
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Old School Reports deleted in {} sec", diff);

        startTime = System.currentTimeMillis();
        logger.debug("Retrieve schools for Non Grad Yearly Distribution");
        StudentSearchRequest searchRequest = getStudentSearchRequest();
        List<String> eligibleStudentSchoolDistricts = gradSchoolOfRecordFilter.filterSchoolOfRecords(searchRequest);
        endTime = System.currentTimeMillis();
        diff = (endTime - startTime)/1000;
        logger.debug("Total {} schools after filters in {} sec", eligibleStudentSchoolDistricts.size(), diff);
        if(eligibleStudentSchoolDistricts.isEmpty() ) {
            logger.debug("No filter found, retrieve all districts");
            startTime = System.currentTimeMillis();
            eligibleStudentSchoolDistricts = parallelDataFetch.fetchDistributionRequiredDataDistrictsNonGradYearly();
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
                if(searchRequest != null) {
                    summaryDTO.setStudentSearchRequest(searchRequest);
                }
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
