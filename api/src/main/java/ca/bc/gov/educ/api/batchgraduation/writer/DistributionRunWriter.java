package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.DistributionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class DistributionRunWriter implements ItemWriter<StudentCredentialDistribution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    DistributionSummaryDTO summaryDTO;
    
    @Override
    public void write(List<? extends StudentCredentialDistribution> list) throws Exception {
        if(!list.isEmpty()) {
        	StudentCredentialDistribution cred = list.get(0);
	        summaryDTO.increment(cred.getPaperType());
            LOGGER.debug("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
