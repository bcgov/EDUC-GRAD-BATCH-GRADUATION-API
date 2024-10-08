package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

public class RunSpecialProjectedGradAlgorithmProcessor extends BaseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunSpecialProjectedGradAlgorithmProcessor.class);

	@Override
	@Nullable
	public GraduationStudentRecord process(@NonNull UUID key) throws Exception {
		GraduationStudentRecord item = getItem(key);
		if (item != null) {
			LOGGER.info("Processing partitionData = {}: {}", item.getStudentID(), item.getProgram());
			summaryDTO.setBatchId(batchId);
			return restUtils.processProjectedGradStudent(item, summaryDTO);
		}
		return null;
	}

}
