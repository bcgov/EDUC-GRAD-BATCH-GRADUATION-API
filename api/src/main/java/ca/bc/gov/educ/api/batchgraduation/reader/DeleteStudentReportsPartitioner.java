package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.School;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.util.GradSchoolOfRecordFilter;
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

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

public class DeleteStudentReportsPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(DeleteStudentReportsPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    @Autowired
    GradSchoolOfRecordFilter gradSchoolOfRecordFilter;

    public DeleteStudentReportsPartitioner() {
        super();
    }

    @Override
    public JobExecution getJobExecution() {
        return jobExecution;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        StudentSearchRequest searchRequest = getStudentSearchRequest();
        long startTime = System.currentTimeMillis();
        logger.debug("Filter Schools for deleting student reports");
        List<String> eligibleStudentSchoolDistricts = gradSchoolOfRecordFilter.filterSchoolOfRecords(searchRequest);
        List<String> finalSchoolDistricts = eligibleStudentSchoolDistricts.stream().sorted().toList();
        if(logger.isDebugEnabled()) {
            logger.debug("Final list of eligible District / School codes {}", String.join(", ", finalSchoolDistricts));
        }
        DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobExecution.getExecutionContext().get("distributionSummaryDTO");
        if(summaryDTO == null) {
            summaryDTO = new DistributionSummaryDTO();
            jobExecution.getExecutionContext().put("distributionSummaryDTO", summaryDTO);
        }
        searchRequest.setSchoolOfRecords(finalSchoolDistricts);

        List<UUID> studentGuidsBySearch = restUtils.getStudentIDsBySearchCriteria(searchRequest, summaryDTO);
        List<UUID> finalStudentGuids = restUtils.getStudentIDsForReportProcessing(studentGuidsBySearch.stream().map(UUID::toString).toList(), "ACHV", summaryDTO);
        if(!finalStudentGuids.isEmpty()) {
            searchRequest.setStudentIDs(finalStudentGuids);
        } else {
            searchRequest.getStudentIDs().clear();
        }

        Integer totalStudentReportsCount = searchRequest.getStudentIDs().size();
        School school = new School("ALL_STUDENTS");
        school.setNumberOfStudents(totalStudentReportsCount);
        summaryDTO.getSchools().add(school);
        summaryDTO.setBatchId(jobExecution.getId());
        summaryDTO.setStudentSearchRequest(searchRequest);

        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Total {} student reports after filters in {} sec", totalStudentReportsCount, diff);

        updateBatchJobHistory(createBatchJobHistory(), totalStudentReportsCount.longValue());
        summaryDTO.setReadCount(totalStudentReportsCount);
        summaryDTO.setProcessedCount(0);

        Map<String, ExecutionContext> map = new HashMap<>();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.put(SEARCH_REQUEST, searchRequest);
        executionContext.put("data", finalStudentGuids);
        executionContext.put("summary", summaryDTO);
        executionContext.put("readCount", 0);
        map.put("partition0", executionContext);

        logger.info("Found {} in total running on 1 partitions", totalStudentReportsCount);
        return map;
    }
}
