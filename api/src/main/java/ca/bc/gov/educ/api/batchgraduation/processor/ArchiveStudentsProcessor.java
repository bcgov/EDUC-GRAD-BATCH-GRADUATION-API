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

public class ArchiveStudentsProcessor implements ItemProcessor<List<UUID>, List<UUID>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStudentsProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Override
	public List<UUID> process(List<UUID> schoolIds) throws Exception {
		Long batchId = summaryDTO.getBatchId();
		StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
		boolean processAllStudents = "ALL".equalsIgnoreCase(searchRequest.getActivityCode());
		long countArchivedStudents = 0l;
		List<String> studentStatusCodes = searchRequest.getStatuses();
		if(schoolIds != null && !schoolIds.isEmpty()) {
			LOGGER.debug("Process Schools: {}", String.join(",", schoolIds.toString()));
			if(studentStatusCodes != null && !studentStatusCodes.isEmpty()) {
				for (String studentStatusCode : studentStatusCodes) {
					countArchivedStudents += restUtils.archiveStudents(batchId, schoolIds, studentStatusCode, summaryDTO);
				}
			} else {
				countArchivedStudents += restUtils.archiveStudents(batchId, schoolIds, "CUR", summaryDTO);
				countArchivedStudents += restUtils.archiveStudents(batchId, schoolIds, "TER", summaryDTO);
			}
		} else if(processAllStudents) {
			LOGGER.debug("Process All Students");
			if(studentStatusCodes != null && !studentStatusCodes.isEmpty()) {
				for (String studentStatusCode : studentStatusCodes) {
					countArchivedStudents += restUtils.archiveStudents(batchId, List.of(), studentStatusCode, summaryDTO);
				}
			} else {
				countArchivedStudents += restUtils.archiveStudents(batchId, List.of(), "CUR", summaryDTO);
				countArchivedStudents += restUtils.archiveStudents(batchId, List.of(), "TER", summaryDTO);
			}
		}
		summaryDTO.setProcessedCount(countArchivedStudents);
		return schoolIds;
	}
}
