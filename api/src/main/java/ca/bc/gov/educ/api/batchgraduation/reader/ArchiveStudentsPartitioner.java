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

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

public class ArchiveStudentsPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveStudentsPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    @Autowired
    GradSchoolOfRecordFilter gradSchoolOfRecordFilter;

    public ArchiveStudentsPartitioner() {
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
        logger.debug("Filter Schools for archiving students");
        boolean processAllStudents = "ALL".equalsIgnoreCase(searchRequest.getActivityCode());
        List<String> eligibleStudentSchoolDistricts = gradSchoolOfRecordFilter.filterSchoolOfRecords(searchRequest);
        List<String> finalSchoolDistricts = eligibleStudentSchoolDistricts.stream().sorted().toList();
        if(logger.isDebugEnabled()) {
            logger.debug("Final list of eligible District / School codes {}", String.join(", ", finalSchoolDistricts));
        }

        summaryDTO.setBatchId(jobExecution.getId());
        summaryDTO.setStudentSearchRequest(searchRequest);

        List<String> studentStatusCodes = searchRequest.getStatuses();
        Long totalStudentsCount = 0L;
        for(String schoolOfRecord: finalSchoolDistricts) {
            Long schoolStudentCount = 0L;
            if(studentStatusCodes != null && !studentStatusCodes.isEmpty()) {
                for(String studentStatusCode: studentStatusCodes) {
                    schoolStudentCount += restUtils.getTotalStudentsBySchoolOfRecordAndStudentStatus(List.of(schoolOfRecord), studentStatusCode, summaryDTO);
                }
            } else {
                schoolStudentCount += restUtils.getTotalStudentsBySchoolOfRecordAndStudentStatus(List.of(schoolOfRecord), "CUR", summaryDTO);
                schoolStudentCount += restUtils.getTotalStudentsBySchoolOfRecordAndStudentStatus(List.of(schoolOfRecord), "TER", summaryDTO);
            }
            School school = new School(schoolOfRecord);
            school.setNumberOfStudents(schoolStudentCount);
            summaryDTO.getSchools().add(school);
            totalStudentsCount += schoolStudentCount;
        }
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Total {} schools after filters in {} sec", eligibleStudentSchoolDistricts.size(), diff);

        if(processAllStudents && finalSchoolDistricts.isEmpty()) {
            Long schoolStudentCount = 0L;
            if(studentStatusCodes != null && !studentStatusCodes.isEmpty()) {
                for(String studentStatusCode: studentStatusCodes) {
                    schoolStudentCount += restUtils.getTotalStudentsBySchoolOfRecordAndStudentStatus(List.of(), studentStatusCode, summaryDTO);
                }
            } else {
                schoolStudentCount += restUtils.getTotalStudentsBySchoolOfRecordAndStudentStatus(List.of(), "CUR", summaryDTO);
                schoolStudentCount += restUtils.getTotalStudentsBySchoolOfRecordAndStudentStatus(List.of(), "TER", summaryDTO);
            }
            totalStudentsCount = schoolStudentCount;
        }

        updateBatchJobHistory(createBatchJobHistory(), totalStudentsCount);
        summaryDTO.setReadCount(totalStudentsCount);
        summaryDTO.setProcessedCount(0);

        Map<String, ExecutionContext> map = new HashMap<>();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.put(SEARCH_REQUEST, searchRequest);
        executionContext.put("data", finalSchoolDistricts);
        executionContext.put("summary", summaryDTO);
        executionContext.put("readCount", 0);
        map.put("partition0", executionContext);

        logger.info("Found {} in total running on 1 partitions", totalStudentsCount);
        return map;
    }
}
