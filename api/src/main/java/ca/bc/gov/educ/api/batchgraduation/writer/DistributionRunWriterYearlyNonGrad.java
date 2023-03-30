package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class DistributionRunWriterYearlyNonGrad implements ItemWriter<List<StudentCredentialDistribution>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunWriterYearlyNonGrad.class);

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;

    @Override
    public void write(List<? extends List<StudentCredentialDistribution>> list) throws Exception {
        if(!list.isEmpty()) {
            List<StudentCredentialDistribution> credentialsList = list.get(0);
            if(credentialsList != null && !credentialsList.isEmpty()) {
                summaryDTO.increment(credentialsList.get(0).getPen());
                LOGGER.debug("Left:{}\n", summaryDTO.getReadCount() - summaryDTO.getProcessedCount());
            }
        }
    }
}
