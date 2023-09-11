package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.PsiDistributionSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

public class PsiDistributionRunWriter implements ItemWriter<PsiCredentialDistribution> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PsiDistributionRunWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    PsiDistributionSummaryDTO summaryDTO;
    
    @Override
    public void write(Chunk<? extends PsiCredentialDistribution> list) throws Exception {
        if(!list.isEmpty()) {
	        summaryDTO.increment("YED4");
            LOGGER.debug("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }
}
