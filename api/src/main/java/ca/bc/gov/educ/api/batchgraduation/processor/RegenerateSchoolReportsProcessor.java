package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class RegenerateSchoolReportsProcessor implements ItemProcessor<List<String>, List<String>> {

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Override
	public List<String> process(List<String> minCodes) throws Exception {
		Long batchId = summaryDTO.getBatchId();
		StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
		long countRegeneratedSchoolReports = 0l;
		List<String> reportTypes = searchRequest.getReportTypes();
		if(log.isDebugEnabled()) {
			log.debug("Process Schools: {}", !minCodes.isEmpty() ? String.join(",", minCodes) : summaryDTO.getSchools().stream().map(School::getMincode).collect(Collectors.joining(",")));
		}

		String reportType;
		if(reportTypes != null && !reportTypes.isEmpty() && "NONGRADPRJ".compareToIgnoreCase(reportTypes.get(0)) == 0)
			reportType = "TVRRUN";
		else
			reportType = "REGALG";

		for (String mincode : minCodes) {
			countRegeneratedSchoolReports += restUtils.createAndStoreSchoolReports(minCodes, reportType, summaryDTO);
		}

		summaryDTO.setProcessedCount(countRegeneratedSchoolReports);
		return minCodes;
	}
}
