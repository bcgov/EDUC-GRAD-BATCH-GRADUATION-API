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

import java.util.*;

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
        boolean processAllReports = "ALL".equalsIgnoreCase(searchRequest.getActivityCode());
        List<String> eligibleStudentSchoolDistricts = gradSchoolOfRecordFilter.filterSchoolOfRecords(searchRequest);
        List<String> finalSchoolDistricts = eligibleStudentSchoolDistricts.stream().sorted().toList();
        if(logger.isDebugEnabled()) {
            logger.debug("Final list of eligible District / School codes {}", String.join(", ", finalSchoolDistricts));
        }
        DistributionSummaryDTO distributionSummaryDTO = (DistributionSummaryDTO)jobExecution.getExecutionContext().get("distributionSummaryDTO");
        if(distributionSummaryDTO == null) {
            distributionSummaryDTO = new DistributionSummaryDTO();
            jobExecution.getExecutionContext().put("distributionSummaryDTO", distributionSummaryDTO);
        }
        searchRequest.setSchoolOfRecords(finalSchoolDistricts);
        if(searchRequest.getReportTypes().isEmpty()) {
            searchRequest.getReportTypes().add("ACHV");
        }

        List<UUID> finalStudentGuids = new ArrayList<>();
        if(processAllReports) {
            for(String reportType: searchRequest.getReportTypes()) {
                List<UUID> reportTypeGuids = restUtils.getReportStudentIDsByStudentIDsAndReportType(List.of(), reportType, distributionSummaryDTO);
                finalStudentGuids.addAll(reportTypeGuids);
            }
        } else {
            List<UUID> studentGuidsBySearch = restUtils.getStudentIDsBySearchCriteriaOrAll(searchRequest, distributionSummaryDTO);
            for(String reportType: searchRequest.getReportTypes()) {
                List<UUID> reportTypeGuids = restUtils.getReportStudentIDsByStudentIDsAndReportType(studentGuidsBySearch.stream().map(UUID::toString).toList(), reportType, distributionSummaryDTO);
                finalStudentGuids.addAll(reportTypeGuids);
            }
        }
        searchRequest.setStudentIDs(finalStudentGuids);

        Integer totalStudentReportsCount = finalStudentGuids.size();
        distributionSummaryDTO.setBatchId(jobExecution.getId());
        distributionSummaryDTO.setStudentSearchRequest(searchRequest);

        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Total {} student reports after filters in {} sec", totalStudentReportsCount, diff);

        updateBatchJobHistory(createBatchJobHistory(), totalStudentReportsCount.longValue());
        distributionSummaryDTO.setReadCount(0);
        distributionSummaryDTO.setProcessedCount(0);

        Map<String, ExecutionContext> map;

        if(processAllReports && totalStudentReportsCount > 0) {
            //proceed with all reports
            finalStudentGuids.clear();
            map = new HashMap<>();
            ExecutionContext executionContext = new ExecutionContext();
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            School school = new School("ALL_STUDENTS");
            school.setNumberOfStudents(totalStudentReportsCount);
            summaryDTO.getSchools().add(school);
            summaryDTO.setBatchId(jobExecution.getId());
            summaryDTO.setReadCount(totalStudentReportsCount);
            summaryDTO.setStudentSearchRequest(searchRequest);
            executionContext.put(SEARCH_REQUEST, searchRequest);
            executionContext.put("data", finalStudentGuids);
            executionContext.put("summary", summaryDTO);
            executionContext.put("readCount", 0);
            map.put("partition0", executionContext);
        } else if (totalStudentReportsCount > 0) {
            int partitionSize = finalStudentGuids.size()/gridSize + 1;
            List<List<UUID>> partitions = new LinkedList<>();
            for (int i = 0; i < finalStudentGuids.size(); i += partitionSize) {
                partitions.add(finalStudentGuids.subList(i, Math.min(i + partitionSize, finalStudentGuids.size())));
            }
            map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
                List<UUID> data = partitions.get(i);
                School school = new School("" + i);
                school.setNumberOfStudents(data.size());
                summaryDTO.getSchools().add(school);
                executionContext.put("data", data);
                summaryDTO.setBatchId(jobExecution.getId());
                summaryDTO.setReadCount(0);
                summaryDTO.setStudentSearchRequest(searchRequest);
                executionContext.put("summary", summaryDTO);
                executionContext.put("readCount", 0);
                executionContext.put("index",i);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
        } else {
            map = new HashMap<>();
        }
        logger.info("Found {} in total running on 1 partitions", totalStudentReportsCount);
        return map;
    }
}
