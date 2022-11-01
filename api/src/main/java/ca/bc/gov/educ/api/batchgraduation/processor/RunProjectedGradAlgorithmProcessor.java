package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RunProjectedGradAlgorithmProcessor extends BaseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunProjectedGradAlgorithmProcessor.class);

	@Override
	public GraduationStudentRecord process(UUID key) throws Exception {
		GraduationStudentRecord item = getItem(key);
		if (item != null) {
			LOGGER.info("*** {} processing partitionData = {}",Thread.currentThread().getName(), item.getProgram());
			summaryDTO.setBatchId(batchId);
			return restUtils.processProjectedGradStudent(item, summaryDTO);
		}
		return null;
	}

    
}
