package ca.bc.gov.educ.api.batchgraduation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmResponse;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.ProcessError;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;

@Service
public class GradAlgorithmService extends GradService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GradAlgorithmService.class);
	
	private final RestUtils restUtils;

	public GradAlgorithmService(RestUtils restUtils) {
		this.restUtils = restUtils;
	}

	public GraduationStudentRecord processStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
		LOGGER.info(" Processing  **** STUDENT ID: ****" + item.getStudentID().toString().substring(5));
		summary.setProcessedCount(summary.getProcessedCount() + 1L);
		try {
			String accessToken = summary.getAccessToken();
			start();
			AlgorithmResponse algorithmResponse = restUtils.runGradAlgorithm(item.getStudentID(), accessToken);
			end();
			return algorithmResponse.getGraduationStudentRecord();
		}catch(Exception e) {
			ProcessError error = new ProcessError();
			error.setStudentID(item.getStudentID().toString());
			error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
			summary.getErrors().add(error);
			summary.setProcessedCount(summary.getProcessedCount() - 1L);
			return null;
		}
		
	}

	
}
