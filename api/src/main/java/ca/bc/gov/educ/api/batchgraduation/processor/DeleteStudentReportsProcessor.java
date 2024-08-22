package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

public class DeleteStudentReportsProcessor implements ItemProcessor<List<UUID>, List<UUID>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeleteStudentReportsProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Override
	public List<UUID> process(List<UUID> uuids) throws Exception {
		Long batchId = summaryDTO.getBatchId();
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Process Student Reports: {}", uuids.size());
		}
		long countDeletedStudentReports = summaryDTO.getProcessedCount();
		StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
		for(String reportType: searchRequest.getReportTypes()) {
			countDeletedStudentReports += restUtils.deleteStudentReports(batchId, uuids, reportType, summaryDTO);
		}
		summaryDTO.setProcessedCount(countDeletedStudentReports);
		return uuids;
	}
}
