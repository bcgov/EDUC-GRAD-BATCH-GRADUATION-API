package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.BlankCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.BlankDistributionSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class BlankDistributionRunWriter implements ItemWriter<BlankCredentialDistribution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlankDistributionRunWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    BlankDistributionSummaryDTO summaryDTO;
    
    @Override
    public void write(List<? extends BlankCredentialDistribution> list) throws Exception {
        if(!list.isEmpty()) {
            BlankCredentialDistribution cred = list.get(0);
	        summaryDTO.increment(cred.getCredentialTypeCode());
            LOGGER.info("Left : {}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
