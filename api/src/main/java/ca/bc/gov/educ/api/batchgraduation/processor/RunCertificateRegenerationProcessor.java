package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecordDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.UUID;

public class RunCertificateRegenerationProcessor implements ItemProcessor<StudentCredentialDistribution, Integer> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunCertificateRegenerationProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	AlgorithmSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	@Nullable
	public Integer process(@NotNull StudentCredentialDistribution item) throws Exception {
		if (item.getStudentID() != null) {
			LOGGER.info("Processing partitionData: studentID = {}", item.getStudentID());
			summaryDTO.setBatchId(batchId);
			try {
				if (StringUtils.isBlank(item.getPen())) {
					item.setPen(getPenNumber(item.getStudentID()));
				}
				summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + 1L);
				Integer count = restUtils.runRegenerateStudentCertificate(item.getPen());
				if (count > 0) {
					restUtils.updateStudentGradRecord(item.getStudentID(), batchId, "CERTREGEN");
				}
				return count;
			} catch(Exception e) {
				LOGGER.error("Unexpected Error: {}", e.getLocalizedMessage());
				summaryDTO.updateError(item.getStudentID(), "Unexpected Error in GRAD-GRADUATION-API", e.getLocalizedMessage());
				summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() - 1L);
				LOGGER.info("Failed STU-ID:{} Errors:{}",item.getStudentID(),summaryDTO.getErrors().size());
				return null;
			}
		}
		LOGGER.warn("Skipped STU-ID:null for pen# {},Errors:{}",item.getPen(),summaryDTO.getErrors().size());
		return null;
	}

	private String getPenNumber(UUID studentID) throws IOException {
		GraduationStudentRecordDistribution stuRec = null;
		try {
			stuRec = restUtils.getStudentData(studentID.toString());
		} catch (RuntimeException se) {
			LOGGER.error("Unknown exception occurred in PEN API: {}", se.getLocalizedMessage());
		}
		if (stuRec != null) {
			return stuRec.getPen();
		}
		throw new IOException(String.format("Student not found in PEN API: STU-ID:%s", studentID));
	}
}
