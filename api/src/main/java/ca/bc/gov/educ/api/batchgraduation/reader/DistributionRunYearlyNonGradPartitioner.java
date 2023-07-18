package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.District;
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
import java.util.stream.Collectors;

public class DistributionRunYearlyNonGradPartitioner extends BasePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyNonGradPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution context;

    @Autowired
    RestUtils restUtils;

    @Autowired
    ParallelDataFetch parallelDataFetch;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // Clean up existing reports before running new one
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_SCHL", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "ADDRESS_LABEL_YE", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "NONGRADDISTREP_SC", restUtils.getAccessToken());
        restUtils.deleteSchoolReportRecord("", "NONGRADDISTREP_SD", restUtils.getAccessToken());

        List<String> eligibleStudentDistricts = parallelDataFetch.fetchDistributionRequiredDataDistrictsNonGradYearly(restUtils.getAccessToken());
        StudentSearchRequest searchRequest = getStudentSearchRequest();
        if(searchRequest != null && searchRequest.getSchoolCategoryCodes() != null && !searchRequest.getSchoolCategoryCodes().isEmpty()) {
            List<String> useFilterDistricts = new ArrayList<>();
            for(String schoolCategoryCode: searchRequest.getSchoolCategoryCodes()) {
                List<District> districts = restUtils.getDistrictBySchoolCategoryCode(schoolCategoryCode);
                for(District district: districts) {
                    useFilterDistricts.add(district.getDistrictNumber());
                }
            }
            eligibleStudentDistricts = eligibleStudentDistricts.stream().distinct().filter(useFilterDistricts::contains).collect(Collectors.toList());
        }
        if(searchRequest != null && searchRequest.getDistricts() != null && !searchRequest.getDistricts().isEmpty()) {
            eligibleStudentDistricts = eligibleStudentDistricts.stream().distinct().filter(searchRequest.getDistricts()::contains).collect(Collectors.toList());
        }
        List<String> finalDistricts = eligibleStudentDistricts.stream().sorted().toList();
        if(!finalDistricts.isEmpty()) {
            updateBatchJobHistory(createBatchJobHistory(), (long) finalDistricts.size());
            int partitionSize = finalDistricts.size();
            List<List<String>> partitions = new LinkedList<>();
            for (int i = 0; i < finalDistricts.size(); i += partitionSize) {
                partitions.add(finalDistricts.subList(i, Math.min(i + partitionSize, finalDistricts.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
                summaryDTO.initializeCredentialCountMap();
                summaryDTO.setCredentialType("NONGRADDIST");
                List<String> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                executionContext.put("index",0);
                executionContext.put("searchRequestObject", searchRequest);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            LOGGER.info("Found {} in total running on {} partitions",finalDistricts.size(),map.size());
            return map;
        }
        LOGGER.info("No Credentials Found for Processing");
        return new HashMap<>();
    }

    @Override
    protected JobExecution getJobExecution() {
        return context;
    }
}
