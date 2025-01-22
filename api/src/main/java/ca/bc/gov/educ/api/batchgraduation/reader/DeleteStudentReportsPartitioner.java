package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
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

public class DeleteStudentReportsPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(DeleteStudentReportsPartitioner.class);

    public static final Integer DEFAULT_ROW_COUNT = 25000;

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
        DistributionSummaryDTO distributionSummaryDTO = (DistributionSummaryDTO)jobExecution.getExecutionContext().get("distributionSummaryDTO");
        if(distributionSummaryDTO == null) {
            distributionSummaryDTO = new DistributionSummaryDTO();
            jobExecution.getExecutionContext().put("distributionSummaryDTO", distributionSummaryDTO);
        }

        StudentSearchRequest searchRequest = getStudentSearchRequest();
        long startTime = System.currentTimeMillis();
        logger.debug("Filter Schools for deleting student reports");
        boolean processAllReports = "ALL".equalsIgnoreCase(searchRequest.getActivityCode());
        Long batchId = jobExecution.getId();
        List<UUID> eligibleStudentSchoolDistricts = gradSchoolOfRecordFilter.filterSchoolsByStudentSearch(searchRequest);
        List<UUID> finalSchoolDistricts = eligibleStudentSchoolDistricts.stream().sorted().toList();
        if(logger.isDebugEnabled()) {
            logger.debug("Final list of eligible District / School codes {}", String.join(", ", finalSchoolDistricts.toString()));
        }

        searchRequest.setSchoolIds(finalSchoolDistricts);
        if(searchRequest.getReportTypes().isEmpty()) {
            searchRequest.getReportTypes().add("ACHV");
        }

        Long totalStudentReportsCount = 0L;

        BatchGradAlgorithmJobHistoryEntity algorithmJobHistory = createBatchJobHistory();

        List<UUID> finalStudentGuids = new ArrayList<>();
        if(processAllReports) {
            for(String reportType: searchRequest.getReportTypes()) {
                Long studentReportsCount = restUtils.getTotalReportsForProcessing(List.of(), reportType, distributionSummaryDTO);
                Integer guidsRowCount = Integer.min(studentReportsCount.intValue(), DEFAULT_ROW_COUNT);
                totalStudentReportsCount += guidsRowCount;
                updateBatchJobHistory(algorithmJobHistory, totalStudentReportsCount);
                List<UUID> reportTypeGuids = restUtils.getReportStudentIDsByStudentIDsAndReportType(List.of(), reportType, guidsRowCount, distributionSummaryDTO);
                finalStudentGuids.addAll(reportTypeGuids);
            }
        } else {
            List<UUID> studentGuidsBySearch = restUtils.getStudentIDsBySearchCriteriaOrAll(searchRequest, distributionSummaryDTO);
            if(!studentGuidsBySearch.isEmpty()) {
                for (String reportType : searchRequest.getReportTypes()) {
                    Long studentReportsCount = restUtils.getTotalReportsForProcessing(studentGuidsBySearch, reportType, distributionSummaryDTO);
                    Integer guidsRowCount = Integer.min(studentReportsCount.intValue(), DEFAULT_ROW_COUNT);
                    totalStudentReportsCount += guidsRowCount;
                    updateBatchJobHistory(algorithmJobHistory, totalStudentReportsCount);
                    List<UUID> reportTypeGuids = restUtils.getReportStudentIDsByStudentIDsAndReportType(studentGuidsBySearch, reportType, guidsRowCount, distributionSummaryDTO);
                    finalStudentGuids.addAll(reportTypeGuids);
                }
            }
        }

        searchRequest.setStudentIDs(finalStudentGuids);
        distributionSummaryDTO.setBatchId(batchId);
        distributionSummaryDTO.setStudentSearchRequest(searchRequest);

        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Total {} student reports after filters in {} sec", totalStudentReportsCount, diff);

        distributionSummaryDTO.setReadCount(0);
        distributionSummaryDTO.setProcessedCount(0);

        Map<String, ExecutionContext> map;

        List<List<UUID>> partitions = new LinkedList<>();
        if(totalStudentReportsCount > 0) {
            int partitionSize = Integer.min(finalStudentGuids.size()/gridSize + 1, 1000);
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
                summaryDTO.setBatchId(batchId);
                summaryDTO.setReadCount(0);
                summaryDTO.setStudentSearchRequest(searchRequest);
                executionContext.put("data", data);
                executionContext.put("summary", summaryDTO);
                executionContext.put("readCount", 0);
                executionContext.put("index",i);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
        } else {
            map = new HashMap<>();
        }
        logger.info("Found {} in total running on {} partitions", totalStudentReportsCount, partitions.size());
        return map;
    }
}
