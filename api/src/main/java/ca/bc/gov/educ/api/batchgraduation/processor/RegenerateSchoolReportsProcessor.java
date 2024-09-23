package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class RegenerateSchoolReportsProcessor implements ItemProcessor<String, String> {

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	SchoolReportsRegenSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public String process(String minCode) throws Exception {
		summaryDTO.setBatchId(batchId);
		if(log.isDebugEnabled()) {
			log.debug("Processing {} School Report: {} ", summaryDTO.getReportBatchType(), minCode);
		}

		long countRegeneratedSchoolReports = restUtils.createAndStoreSchoolReports(minCode, summaryDTO.getReportBatchType(), summaryDTO);

		summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + countRegeneratedSchoolReports);
		return minCode;
	}
}
