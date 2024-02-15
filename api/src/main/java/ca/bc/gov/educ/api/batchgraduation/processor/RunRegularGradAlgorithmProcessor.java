package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

public class RunRegularGradAlgorithmProcessor extends BaseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunRegularGradAlgorithmProcessor.class);

	@Override
	@Nullable
	public GraduationStudentRecord process(@NonNull UUID key) throws Exception {
		GraduationStudentRecord item = getItem(key);
		if (item != null) {
			LOGGER.info("Processing partitionData = {}", item.getProgram());
			summaryDTO.setBatchId(batchId);
			return restUtils.processStudent(item, summaryDTO);
		}
		return null;
	}

    
}
