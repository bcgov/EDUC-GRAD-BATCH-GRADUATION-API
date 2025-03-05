package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

@Slf4j
public class RegenerateSchoolReportsProcessor implements ItemProcessor<UUID, UUID> {

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	SchoolReportsRegenSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public UUID process(@NonNull UUID schoolId) throws Exception {
		summaryDTO.setBatchId(batchId);
		if(log.isDebugEnabled()) {
			log.debug("Processing {} School Report: {} ", summaryDTO.getReportBatchType(), schoolId);
		}

		long countRegeneratedSchoolReports = restUtils.createAndStoreSchoolReports(schoolId, summaryDTO.getReportBatchType(), summaryDTO);

		summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + countRegeneratedSchoolReports);
		return schoolId;
	}
}
