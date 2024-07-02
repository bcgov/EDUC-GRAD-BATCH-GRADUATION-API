package ca.bc.gov.educ.api.batchgraduation.processor;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.*;

public class RunSpecialProjectedGradAlgorithmProcessor extends BaseProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(RunSpecialProjectedGradAlgorithmProcessor.class);

	@Value("#{stepExecution.jobExecution}")
	JobExecution jobExecution;

	@Override
	@Nullable
	public GraduationStudentRecord process(@NonNull UUID key) throws Exception {
		JobParameters jobParameters = jobExecution.getJobParameters();
		String searchRequest = jobParameters.getString(SEARCH_REQUEST, "{}");
		StudentSearchRequest req = (StudentSearchRequest)jsonTransformer.unmarshall(searchRequest, StudentSearchRequest.class);
		GraduationStudentRecord item = getItem(key);
		if (item != null) {
			LOGGER.info("Processing partitionData = {}: {}", item.getStudentID(), item.getProgram());
			summaryDTO.setBatchId(batchId);
			if(StringUtils.equalsAnyIgnoreCase(req.getActivityCode(), TVRCREATE, TVRUPDATE, TVRDELETE)) {
				Integer count = restUtils.processStudentReports(List.of(key), "TVRRUN", req.getActivityCode());
				if(count > 0) {
					restUtils.updateStudentGradRecord(key, jobExecution.getId(), StringUtils.upperCase(req.getActivityCode()));
				}
			} else {
				return restUtils.processProjectedGradStudent(item, summaryDTO);
			}
		}
		return item;
	}

}
