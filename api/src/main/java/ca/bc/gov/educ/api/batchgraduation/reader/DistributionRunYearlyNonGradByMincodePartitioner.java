package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.ParallelDataFetch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DistributionRunYearlyNonGradByMincodePartitioner extends SimplePartitioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyNonGradByMincodePartitioner.class);

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

        List<String> schoolsList = parallelDataFetch.fetchDistributionRequiredDataDistrictsNonGradYearly(restUtils.getAccessToken());
        if(!schoolsList.isEmpty()) {

            int partitionSize = schoolsList.size();
            List<List<String>> partitions = new LinkedList<>();
            for (int i = 0; i < schoolsList.size(); i += partitionSize) {
                partitions.add(schoolsList.subList(i, Math.min(i + partitionSize, schoolsList.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
                summaryDTO.initializeCredentialCountMap();
                List<String> data = partitions.get(i);
                executionContext.put("data", data);
                summaryDTO.setReadCount(data.size());
                executionContext.put("summary", summaryDTO);
                executionContext.put("index",0);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            LOGGER.info("Found {} in total running on {} partitions",schoolsList.size(),map.size());
            return map;
        }
        LOGGER.info("No Credentials Found for Processing");
        return new HashMap<>();
    }
}
