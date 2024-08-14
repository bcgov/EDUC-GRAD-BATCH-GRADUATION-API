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

public class ArchiveStudentsProcessor implements ItemProcessor<List<String>, List<String>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStudentsProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Override
	public List<String> process(List<String> minCodes) throws Exception {
		Long batchId = summaryDTO.getBatchId();
		StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
		long countArchivedStudents = 0l;
		List<String> studentStatusCodes = searchRequest.getStatuses();
		if(minCodes != null && !minCodes.isEmpty()) {
			LOGGER.debug("Process Schools: {}", String.join(",", minCodes));
			if(studentStatusCodes != null && !studentStatusCodes.isEmpty()) {
				for (String studentStatusCode : studentStatusCodes) {
					countArchivedStudents += restUtils.archiveStudents(batchId, minCodes, studentStatusCode, summaryDTO);
				}
			} else {
				countArchivedStudents += restUtils.archiveStudents(batchId, minCodes, "CUR", summaryDTO);
				countArchivedStudents += restUtils.archiveStudents(batchId, minCodes, "TER", summaryDTO);
			}
		} else {
			LOGGER.debug("Process All Students");
			if(studentStatusCodes != null && !studentStatusCodes.isEmpty()) {
				for (String studentStatusCode : studentStatusCodes) {
					countArchivedStudents += restUtils.archiveStudents(batchId, null, studentStatusCode, summaryDTO);
				}
			} else {
				countArchivedStudents += restUtils.archiveStudents(batchId, null, "CUR", summaryDTO);
				countArchivedStudents += restUtils.archiveStudents(batchId, null, "TER", summaryDTO);
			}
		}
		summaryDTO.setProcessedCount(countArchivedStudents);
		return minCodes;
	}
}
