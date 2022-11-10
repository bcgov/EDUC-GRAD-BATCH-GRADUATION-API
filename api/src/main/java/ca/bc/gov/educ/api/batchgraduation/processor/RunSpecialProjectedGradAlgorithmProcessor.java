package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunSpecialProjectedGradAlgorithmProcessor extends BaseSpecialRunProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunSpecialProjectedGradAlgorithmProcessor.class);

	@Override
	public GraduationStudentRecord process(GraduationStudentRecord item) throws Exception {
		LOGGER.info("Processing partitionData = {}", item.getProgram());
		summaryDTO.setBatchId(batchId);
		return restUtils.processProjectedGradStudent(item, summaryDTO);
	}

}
