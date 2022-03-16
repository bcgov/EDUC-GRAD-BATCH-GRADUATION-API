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
		LOGGER.info("*** {} Partition  - Processing  * STUDENT ID: * {}",Thread.currentThread().getName(),item.getStudentID().toString());
		summary.setProcessedCount(summary.getProcessedCount() + 1L);
		try {
			String accessToken = summary.getAccessToken();
			start();
			AlgorithmResponse algorithmResponse = restUtils.runGradAlgorithm(item.getStudentID(), accessToken,item.getProgramCompletionDate(),summary.getBatchId());
			if(algorithmResponse.getException() != null) {
				ProcessError error = new ProcessError();
				error.setStudentID(item.getStudentID().toString());
				error.setReason(algorithmResponse.getException().getExceptionName());
				error.setDetail(algorithmResponse.getException().getExceptionDetails());
				summary.getErrors().add(error);
				summary.setProcessedCount(summary.getProcessedCount() - 1L);
				return null;
			}
			LOGGER.info("*** {} Partition  * Processed student[{}] * Student ID: {} in total {}",Thread.currentThread().getName(), summary.getProcessedCount(), item.getStudentID(), summary.getReadCount());
			summary.increment(item.getProgram());
			end();
			return algorithmResponse.getGraduationStudentRecord();
		}catch(Exception e) {
			ProcessError error = new ProcessError();
			error.setStudentID(item.getStudentID().toString());
			error.setReason("GRAD-GRADUATION-API IS DOWN");
			error.setDetail("Graduation API is unavialble at this moment");
			summary.getErrors().add(error);
			summary.setProcessedCount(summary.getProcessedCount() - 1L);
			LOGGER.info("*** {} Partition  - Processing Failed  * STUDENT ID: * {}",Thread.currentThread().getName(),item.getStudentID().toString());
			return null;
		}
		
	}

	public GraduationStudentRecord processProjectedGradStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
		LOGGER.info(" Processing  **** STUDENT ID: ****" + item.getStudentID().toString().substring(5));
		summary.setProcessedCount(summary.getProcessedCount() + 1L);
		try {
			String accessToken = summary.getAccessToken();
			start();
			AlgorithmResponse algorithmResponse = restUtils.runProjectedGradAlgorithm(item.getStudentID(), accessToken,summary.getBatchId());
			if(algorithmResponse.getException() != null) {
				ProcessError error = new ProcessError();
				error.setStudentID(item.getStudentID().toString());
				error.setReason(algorithmResponse.getException().getExceptionName());
				error.setDetail(algorithmResponse.getException().getExceptionDetails());
				summary.getErrors().add(error);
				summary.setProcessedCount(summary.getProcessedCount() - 1L);
				return null;
			}
			end();
			return algorithmResponse.getGraduationStudentRecord();
		}catch(Exception e) {
			ProcessError error = new ProcessError();
			error.setStudentID(item.getStudentID().toString());
			error.setReason("GRAD-GRADUATION-API IS DOWN");
			error.setDetail("Graduation API is unavialble at this moment");
			summary.getErrors().add(error);
			summary.setProcessedCount(summary.getProcessedCount() - 1L);
			return null;
		}

	}

	
}
