package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.service.DistributionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class DistributionRunWriter implements ItemWriter<StudentCredentialDistribution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunWriter.class);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    DistributionService distributionService;

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;

    @Value("#{stepExecution.jobExecution}")
    JobExecution jobExecution;
    
    @Override
    public void write(Chunk<? extends StudentCredentialDistribution> list) {
        if(!list.isEmpty()) {
        	StudentCredentialDistribution cred = list.getItems().get(0);
	        summaryDTO.increment(cred.getPaperType());

            // save StudentCredentialDistribution
            JobParameters jobParameters = jobExecution.getJobParameters();
            String jobType = jobParameters.getString("jobType");
            distributionService.saveStudentCredentialDistribution(summaryDTO.getBatchId(), jobType, cred);

            LOGGER.debug("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
