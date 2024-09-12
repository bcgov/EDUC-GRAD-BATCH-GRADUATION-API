package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportsRegenSummaryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Slf4j
public class RegenerateSchoolReportsReader implements ItemReader<List<String>> {

    @Value("#{stepExecutionContext['data']}")
    List<String> schools;

    @Value("#{stepExecutionContext['summary']}")
    SchoolReportsRegenSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;

    @Value("#{stepExecutionContext['readCount']}")
    Long readCount;

    @Override
    public List<String> read() throws Exception {
        if(readCount > 0) return null;
        readCount++;
        if(log.isDebugEnabled()) {
            log.info("Read schools Codes -> {} of {} schools", readCount, schools.size());
        }
        aggregate("schoolReportsRegenSummaryDTO");
        return schools;
    }

    private void aggregate(String summaryContextName) {
        SchoolReportsRegenSummaryDTO totalSummaryDTO = (SchoolReportsRegenSummaryDTO) jobExecution.getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new SchoolReportsRegenSummaryDTO();
            jobExecution.getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }
        totalSummaryDTO.setBatchId(summaryDTO.getBatchId());
        totalSummaryDTO.setReadCount(summaryDTO.getReadCount());
        totalSummaryDTO.getGlobalList().addAll(summaryDTO.getGlobalList());
    }
}
