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

public class ArchiveSchoolReportsProcessor implements ItemProcessor<List<String>, List<String>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveSchoolReportsProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Override
	public List<String> process(List<String> minCodes) throws Exception {
		Long batchId = summaryDTO.getBatchId();
		StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
		long countArchivedSchoolReports = 0l;
		List<String> reportTypes = searchRequest.getReportTypes();
		if(minCodes != null && !minCodes.isEmpty()) {
			LOGGER.debug("Process Schools: {}", String.join(",", minCodes));
			if(reportTypes != null && !reportTypes.isEmpty()) {
				for (String reportTypeCode : reportTypes) {
					countArchivedSchoolReports += restUtils.archiveSchoolReports(batchId, minCodes, reportTypeCode, summaryDTO);
				}
			}
		} else {
			LOGGER.debug("Process All Schools");
			if(reportTypes != null && !reportTypes.isEmpty()) {
				for (String reportTypeCode : reportTypes) {
					countArchivedSchoolReports += restUtils.archiveSchoolReports(batchId, null, reportTypeCode, summaryDTO);
				}
			}
		}
		summaryDTO.setProcessedCount(countArchivedSchoolReports);
		return minCodes;
	}
}
