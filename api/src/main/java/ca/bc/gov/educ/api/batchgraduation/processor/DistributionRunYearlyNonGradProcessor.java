package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public class DistributionRunYearlyNonGradProcessor implements ItemProcessor<UUID, List<StudentCredentialDistribution>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyNonGradProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	DistributionSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public List<StudentCredentialDistribution> process(@NonNull UUID schoolId) throws Exception {
		summaryDTO.setBatchId(batchId);
		LOGGER.debug("Processing partitionData for school {} ", schoolId);
		summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + 1L);
		List<StudentCredentialDistribution> studentCredentials = restUtils.fetchDistributionRequiredDataStudentsNonGradYearly(schoolId);
		StudentSearchRequest searchRequest = summaryDTO.getStudentSearchRequest();
		if(searchRequest != null && searchRequest.getPens() != null && !searchRequest.getPens().isEmpty()) {
			studentCredentials.removeIf(scr->!searchRequest.getPens().contains(scr.getPen()));
		}
		LOGGER.debug("Completed partitionData for school {} with {} students", schoolId, studentCredentials.size());
		summaryDTO.getGlobalList().addAll(studentCredentials);
		return studentCredentials;
	}
}
