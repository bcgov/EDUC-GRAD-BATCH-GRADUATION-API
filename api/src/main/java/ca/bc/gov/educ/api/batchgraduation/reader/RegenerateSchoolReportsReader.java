package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.SchoolReportsRegenSummaryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class RegenerateSchoolReportsReader extends BaseSchoolReader implements ItemReader<String> {

    @Value("#{stepExecutionContext['summary']}")
    SchoolReportsRegenSummaryDTO summaryDTO;

    @Override
    public String read() throws Exception {
        String nextSchool = null;
        if (nextSchoolForProcessing < schools.size()) {
            nextSchool = schools.get(nextSchoolForProcessing);
            log.info("School: {} - {} of {}", nextSchool, nextSchoolForProcessing + 1, summaryDTO.getReadCount());
            nextSchoolForProcessing++;
        } else {
            aggregate("schoolReportsRegenSummaryDTO");
        }
        return nextSchool;
    }

    private void aggregate(String summaryContextName) {
        SchoolReportsRegenSummaryDTO totalSummaryDTO = (SchoolReportsRegenSummaryDTO) jobExecution.getExecutionContext().get(summaryContextName);
        if (totalSummaryDTO == null) {
            totalSummaryDTO = new SchoolReportsRegenSummaryDTO();
            jobExecution.getExecutionContext().put(summaryContextName, totalSummaryDTO);
        }
        totalSummaryDTO.setBatchId(summaryDTO.getBatchId());
        totalSummaryDTO.setProcessedCount(totalSummaryDTO.getProcessedCount() + summaryDTO.getProcessedCount());
        totalSummaryDTO.getGlobalList().addAll(summaryDTO.getGlobalList());
    }
}
