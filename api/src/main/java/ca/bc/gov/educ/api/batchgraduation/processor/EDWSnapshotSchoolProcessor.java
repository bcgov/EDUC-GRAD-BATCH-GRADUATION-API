package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.EdwSnapshotSchoolSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.SnapshotResponse;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class EDWSnapshotSchoolProcessor implements ItemProcessor<String, List<Pair<String, List<SnapshotResponse>>>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EDWSnapshotSchoolProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	EdwSnapshotSchoolSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public List<Pair<String, List<SnapshotResponse>>> process(String mincode) throws Exception {
		summaryDTO.setBatchId(batchId);
		LOGGER.debug("Processing partitionData for School: {} ", mincode);
		List<SnapshotResponse> edwStudents = restUtils.getEDWSnapshotStudents(summaryDTO.getGradYear(), mincode, summaryDTO.getAccessToken());
		return List.of(Pair.of(mincode, edwStudents));
	}
}
