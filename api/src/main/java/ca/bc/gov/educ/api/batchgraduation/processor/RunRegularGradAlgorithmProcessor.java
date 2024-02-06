package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RunRegularGradAlgorithmProcessor extends BaseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunRegularGradAlgorithmProcessor.class);

	@Override
	public GraduationStudentRecord process(UUID key) throws Exception {
		GraduationStudentRecord item = getItem(key);
//		Thread.sleep(60000);
		if (item != null) {
			LOGGER.info("Processing partitionData = {}", item.getProgram());
			summaryDTO.setBatchId(batchId);
			return restUtils.processStudent(item, summaryDTO);
		}
		return null;
	}

    
}
