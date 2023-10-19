package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class EDWSnapshotProcessor implements ItemProcessor<SnapshotResponse, EdwGraduationSnapshot> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	EdwSnapshotSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public EdwGraduationSnapshot process(SnapshotResponse snapshot) throws Exception {
		summaryDTO.setBatchId(batchId);
		LOGGER.debug("Processing partitionData for Snapshot - pen# {} ", snapshot.getPen());
		EdwGraduationSnapshot item = new EdwGraduationSnapshot();
		item.setPen(snapshot.getPen());
		item.setSchoolOfRecord(snapshot.getSchoolOfRecord());
		item.setGraduatedDate(snapshot.getGraduatedDate());
		item.setGpa(snapshot.getGpa());
		item.setHonoursStanding(snapshot.getHonourFlag());
		item.setGradYear(summaryDTO.getGradYear());

		return restUtils.processSnapshot(item, summaryDTO);
	}
}
