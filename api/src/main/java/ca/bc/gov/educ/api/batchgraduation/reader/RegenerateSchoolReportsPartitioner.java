package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.util.GradSchoolOfRecordFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

@Slf4j
public class RegenerateSchoolReportsPartitioner extends BasePartitioner {

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    @Autowired
    GradSchoolOfRecordFilter gradSchoolOfRecordFilter;

    public RegenerateSchoolReportsPartitioner() {
        super();
    }

    @Override
    public JobExecution getJobExecution() {
        return jobExecution;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobExecution.getExecutionContext().get("distributionSummaryDTO");
        if(summaryDTO == null) {
            summaryDTO = new DistributionSummaryDTO();
            jobExecution.getExecutionContext().put("distributionSummaryDTO", summaryDTO);
        }

        StudentSearchRequest searchRequest = getStudentSearchRequest();
        long startTime = System.currentTimeMillis();
        log.debug("Filter Schools for school reports regeneration");
        boolean processAllStudents = "ALL".equalsIgnoreCase(searchRequest.getActivityCode());
        List<String> eligibleStudentSchoolDistricts = gradSchoolOfRecordFilter.filterSchoolOfRecords(searchRequest);
        List<String> finalSchoolDistricts = eligibleStudentSchoolDistricts.stream().sorted().toList();
        if(log.isDebugEnabled()) {
            log.debug("Final list of eligible District / School codes {}", String.join(", ", finalSchoolDistricts));
        }

        summaryDTO.setBatchId(jobExecution.getId());
        summaryDTO.setStudentSearchRequest(searchRequest);

        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        log.debug("Total {} schools after filters in {} sec", finalSchoolDistricts.size(), diff);

        updateBatchJobHistory(createBatchJobHistory(), (long)finalSchoolDistricts.size());
        summaryDTO.setReadCount((long)finalSchoolDistricts.size());
        summaryDTO.setProcessedCount(0);

        Map<String, ExecutionContext> map = new HashMap<>();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.put(SEARCH_REQUEST, searchRequest);
        executionContext.put("data", finalSchoolDistricts);
        executionContext.put("summary", summaryDTO);
        executionContext.put("readCount", 0);
        map.put("partition0", executionContext);

        log.info("Found {} in total running on 1 partitions", finalSchoolDistricts);
        return map;
    }
}
