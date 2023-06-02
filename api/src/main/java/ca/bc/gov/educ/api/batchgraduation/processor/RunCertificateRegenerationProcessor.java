package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecordDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

public class RunCertificateRegenerationProcessor implements ItemProcessor<UUID, Integer> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunCertificateRegenerationProcessor.class);

	@Autowired
	RestUtils restUtils;

	@Value("#{stepExecutionContext['summary']}")
	AlgorithmSummaryDTO summaryDTO;

	@Value("#{stepExecution.jobExecution.id}")
	Long batchId;

	@Override
	public Integer process(UUID key) throws Exception {
		GraduationStudentRecordDistribution stuRec = restUtils.getStudentData(key.toString(), summaryDTO.getAccessToken());
		if (stuRec != null) {
			LOGGER.info("Processing partitionData: studentID = {}", stuRec.getStudentID());
			summaryDTO.setBatchId(batchId);
			try {
				summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() + 1L);
				Integer count = restUtils.runRegenerateStudentCertificate(stuRec.getPen(), summaryDTO.getAccessToken());
				if (count > 0) {
					restUtils.updateStudentGradRecord(key, batchId, "CERTREGEN", summaryDTO.getAccessToken());
				}
				return count;
			} catch(Exception e) {
				LOGGER.error("Unexpected Error: {}", e.getLocalizedMessage());
				summaryDTO.updateError(key,"GRAD-GRADUATION-API IS DOWN","Graduation API is unavailable at this moment");
				summaryDTO.setProcessedCount(summaryDTO.getProcessedCount() - 1L);
				LOGGER.info("Failed STU-ID:{} Errors:{}",key,summaryDTO.getErrors().size());
				return null;
			}
		}
		return null;
	}

}
