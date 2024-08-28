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

public class ArchiveSchoolReportsPartitioner extends BasePartitioner {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveSchoolReportsPartitioner.class);

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    @Autowired
    GradSchoolOfRecordFilter gradSchoolOfRecordFilter;

    public ArchiveSchoolReportsPartitioner() {
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
        logger.debug("Filter Schools for archiving school reports");
        List<String> eligibleStudentSchoolDistricts = gradSchoolOfRecordFilter.filterSchoolOfRecords(searchRequest);
        List<String> finalSchoolDistricts = eligibleStudentSchoolDistricts.stream().sorted().toList();
        if(logger.isDebugEnabled()) {
            logger.debug("Final list of eligible District / School codes {}", String.join(", ", finalSchoolDistricts));
        }

        summaryDTO.setBatchId(jobExecution.getId());
        summaryDTO.setStudentSearchRequest(searchRequest);

        Long totalSchoolReporsCount = 0L;
        List<String> reportTypes = searchRequest.getReportTypes();
        Long schoolReportsCount = 0L;
        if(!finalSchoolDistricts.isEmpty()) {
            for (String schoolOfRecord : finalSchoolDistricts) {
                if (reportTypes != null && !reportTypes.isEmpty()) {
                    for (String reportType : reportTypes) {
                        schoolReportsCount += restUtils.getTotalReportsForProcessing(List.of(schoolOfRecord), reportType, summaryDTO);
                    }
                }
                School school = new School(schoolOfRecord);
                school.setNumberOfSchoolReports(schoolReportsCount);
                summaryDTO.getSchools().add(school);
                totalSchoolReporsCount += schoolReportsCount;
            }
        } else {
            if (reportTypes != null && !reportTypes.isEmpty()) {
                for (String reportType : reportTypes) {
                    schoolReportsCount += restUtils.getTotalReportsForProcessing(List.of(), reportType, summaryDTO);
                }
            }
            School school = new School("ALL_SCHOOLS");
            school.setNumberOfSchoolReports(schoolReportsCount);
            summaryDTO.getSchools().add(school);
            totalSchoolReporsCount += schoolReportsCount;
        }
        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        logger.debug("Total {} schools after filters in {} sec", eligibleStudentSchoolDistricts.size(), diff);

        updateBatchJobHistory(createBatchJobHistory(), totalSchoolReporsCount);
        summaryDTO.setReadCount(totalSchoolReporsCount);
        summaryDTO.setProcessedCount(0);

        Map<String, ExecutionContext> map = new HashMap<>();
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.put(SEARCH_REQUEST, searchRequest);
        executionContext.put("data", finalSchoolDistricts);
        executionContext.put("summary", summaryDTO);
        executionContext.put("readCount", 0);
        map.put("partition0", executionContext);

        logger.info("Found {} in total running on 1 partitions", totalSchoolReporsCount);
        return map;
    }
}
