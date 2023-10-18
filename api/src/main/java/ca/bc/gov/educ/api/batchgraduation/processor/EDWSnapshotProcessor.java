package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

public class EDWSnapshotProcessor implements ItemProcessor<String, List<Pair<String, List<EdwGraduationSnapshot>>>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	EdwSnapshotSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public List<Pair<String, List<EdwGraduationSnapshot>>> process(String mincode) throws Exception {
		summaryDTO.setBatchId(batchId);
		LOGGER.debug("Processing partitionData for mincode {} ", mincode);
		List<SnapshotResponse> edwStudents = restUtils.getEDWSnapshotStudents(summaryDTO.getGradYear(), mincode, summaryDTO.getAccessToken());
		List<EdwGraduationSnapshot> results = edwStudents.stream().map(r -> {
			refreshToken();
			UUID studentID = this.restUtils.getStudentIDByPen(r.getPen(), summaryDTO.getAccessToken());
			EdwGraduationSnapshot m = new EdwGraduationSnapshot();
			m.setStudentID(studentID);
			m.setPen(r.getPen());
			m.setSchoolOfRecord(r.getSchoolOfRecord());
			m.setGraduatedDate(r.getGraduatedDate());
			m.setGpa(r.getGpa());
			m.setHonoursStanding(r.getHonourFlag());
			m.setGradYear(summaryDTO.getGradYear());
			return m;
		}).toList();
		LOGGER.debug("Students found: {}", results.size());
		return List.of(Pair.of(mincode, results));
	}

	private void refreshToken() {
		ResponseObj res = restUtils.getTokenResponseObject();
		if (res != null) {
			summaryDTO.setAccessToken(res.getAccess_token());
		}
	}
}
