package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.List;

public class DistributionRunYearlyNonGradByMincodeWriter extends BaseYearEndWriter implements ItemWriter<List<StudentCredentialDistribution>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyNonGradByMincodeWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution.id}")
    Long jobExecutionId;

    @Value("#{stepExecution.jobExecution.status}")
    BatchStatus status;

    @Value("#{stepExecution.jobExecution.jobParameters}")
    JobParameters jobParameters;

    @Value("#{stepExecution.jobExecution.startTime}")
    Date startTime;

    @Value("#{stepExecution.jobExecution.endTime}")
    Date endTime;

    @Override
    public void write(List<? extends List<StudentCredentialDistribution>> list) throws Exception {
        if (!list.isEmpty()) {
            summaryDTO.increment("YED4");
            LOGGER.debug("Left:{}\n", summaryDTO.getReadCount() - summaryDTO.getProcessedCount());
//            LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
//            processGlobalList(summaryDTO.getGlobalList(), jobExecutionId, summaryDTO.getMapDist(), "NONGRADDIST", restUtils.fetchAccessToken());
//            LOGGER.info("=======================================================================================");
        }
    }
}
