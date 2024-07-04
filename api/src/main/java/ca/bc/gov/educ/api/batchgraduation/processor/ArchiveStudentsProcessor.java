package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ArchiveStudentsProcessor extends BaseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStudentsProcessor.class);

	@Override
	public GraduationStudentRecord process(UUID uuid) throws Exception {
		return null;
	}
}
