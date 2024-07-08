package ca.bc.gov.educ.api.batchgraduation.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

public class ArchiveStudentsProcessor implements ItemProcessor<List<String>, List<String>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArchiveStudentsProcessor.class);

	@Override
	public List<String> process(List<String> minCodes) throws Exception {
		if(minCodes != null && !minCodes.isEmpty()) {
			LOGGER.debug("Process Schools: {}", String.join(",", minCodes));
		} else {
			LOGGER.debug("Process All Students");
		}
		return minCodes;
	}
}
