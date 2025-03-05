package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.util.GradSchoolOfRecordFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.stream.Collectors;

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
        SchoolReportsRegenSummaryDTO summaryDTO = (SchoolReportsRegenSummaryDTO)jobExecution.getExecutionContext().get("schoolReportsRegenSummaryDTO");
        if(summaryDTO == null) {
            summaryDTO = new SchoolReportsRegenSummaryDTO();
            jobExecution.getExecutionContext().put("schoolReportsRegenSummaryDTO", summaryDTO);
        }

        StudentSearchRequest searchRequest = getStudentSearchRequest();
        long startTime = System.currentTimeMillis();
        log.debug("Filter Schools for school reports regeneration");
        boolean processAllReports = "ALL".equalsIgnoreCase(searchRequest.getActivityCode());

        List<UUID> schoolDistricts = gradSchoolOfRecordFilter.filterSchoolsByStudentSearch(searchRequest);
        if(log.isDebugEnabled()) {
            log.debug("Final list of eligible District / School codes {}", String.join(", ", schoolDistricts.toString()));
        }

        summaryDTO.setBatchId(jobExecution.getId());
        summaryDTO.setReportBatchType(determineReportBatchType(searchRequest.getReportTypes()));

        Long totalSchoolReportsCount = 0L;
        Long schoolReportsCount = 0L;

        List<UUID> finalSchoolDistricts = new ArrayList<>();
        List<SchoolReport> schoolReportsLite;

        if(processAllReports) {
            if ("TVRRUN".compareToIgnoreCase(summaryDTO.getReportBatchType()) == 0) {
                schoolReportsLite = restUtils.getSchoolReportsLiteByReportType("NONGRADPRJ", summaryDTO);
            } else {
                schoolReportsLite = restUtils.getSchoolReportsLiteByReportType( "GRADREG", summaryDTO);
            }

            if (schoolReportsLite != null && !schoolReportsLite.isEmpty()) {
                finalSchoolDistricts = schoolReportsLite.stream().map(SchoolReport::getSchoolOfRecordId)
                        .collect(Collectors.toList());
                schoolReportsCount = (long)finalSchoolDistricts.size();
            }

            School school = new School("ALL_SCHOOLS");
            school.setNumberOfSchoolReports(schoolReportsCount);
            summaryDTO.getSchools().add(school);
            totalSchoolReportsCount += finalSchoolDistricts.size();
        } else {
            for (UUID schoolId : schoolDistricts) {
                if ("TVRRUN".compareToIgnoreCase(summaryDTO.getReportBatchType()) == 0) {
                    schoolReportsCount += restUtils.getTotalReportsForProcessing(List.of(schoolId), "NONGRADPRJ", summaryDTO);
                } else {
                    schoolReportsCount += restUtils.getTotalReportsForProcessing(List.of(schoolId), "GRADREG", summaryDTO);
                }
                if (schoolReportsCount > 0) {
                    finalSchoolDistricts.add(schoolId);
                    School school = new School(schoolId);
                    school.setNumberOfSchoolReports(schoolReportsCount);
                    summaryDTO.getSchools().add(school);
                    totalSchoolReportsCount += schoolReportsCount;
                }
                schoolReportsCount = 0L;
            }
        }

        long endTime = System.currentTimeMillis();
        long diff = (endTime - startTime)/1000;
        log.debug("Total {} schools after filters in {} sec", finalSchoolDistricts.size(), diff);

        summaryDTO.setReadCount(totalSchoolReportsCount);
        summaryDTO.setProcessedCount(0);

        if (!finalSchoolDistricts.isEmpty()) {
            updateBatchJobHistory(createBatchJobHistory(), totalSchoolReportsCount);
            int partitionSize = finalSchoolDistricts.size()/gridSize + 1;
            List<List<UUID>> partitions = new LinkedList<>();
            for (int i = 0; i < finalSchoolDistricts.size(); i += partitionSize) {
                partitions.add(finalSchoolDistricts.subList(i, Math.min(i + partitionSize, finalSchoolDistricts.size())));
            }
            Map<String, ExecutionContext> map = new HashMap<>(partitions.size());
            for (int i = 0; i < partitions.size(); i++) {
                ExecutionContext executionContext = new ExecutionContext();
                SchoolReportsRegenSummaryDTO partitionSummaryDTO = new SchoolReportsRegenSummaryDTO();
                partitionSummaryDTO.setReportBatchType(summaryDTO.getReportBatchType());
                List<UUID> data = partitions.get(i);
                executionContext.put("data", data);
                partitionSummaryDTO.setReadCount(data.size());
                executionContext.put("summary", partitionSummaryDTO);
                executionContext.put("index",0);
                String key = "partition" + i;
                map.put(key, executionContext);
            }
            log.info("Found {} in total running on {} partitions",finalSchoolDistricts.size(),map.size());
            return map;
        }
        log.info("No Schools Found for School Reports Regeneration");
        return new HashMap<>();
    }

    private String determineReportBatchType(List<String> reportTypes) {
        return reportTypes != null && !reportTypes.isEmpty() && "NONGRADPRJ".compareToIgnoreCase(reportTypes.get(0)) == 0 ? "TVRRUN" : "REGALG";
    }
}
