package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.School;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ArchiveSchoolReportsProcessor implements ItemProcessor<List<UUID>, List<UUID>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveSchoolReportsProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Override
	public List<UUID> process(List<UUID> schoolIds) throws Exception {
		Long batchId = summaryDTO.getBatchId();
		StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
		long countArchivedSchoolReports = 0l;
		List<String> reportTypes = searchRequest.getReportTypes();
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Process Schools: {}", !schoolIds.isEmpty() ? String.join(",", schoolIds.toString()) : summaryDTO.getSchools().stream().map(School::getSchoolId).collect(Collectors.joining(",")));
		}
		if(reportTypes != null && !reportTypes.isEmpty()) {
			for (String reportTypeCode : reportTypes) {
				countArchivedSchoolReports += restUtils.archiveSchoolReports(batchId, schoolIds, reportTypeCode, summaryDTO);
			}
		}
		summaryDTO.setProcessedCount(countArchivedSchoolReports);
		return schoolIds;
	}
}
