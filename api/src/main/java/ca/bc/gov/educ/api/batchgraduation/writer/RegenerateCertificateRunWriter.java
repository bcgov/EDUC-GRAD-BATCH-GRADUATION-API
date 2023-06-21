package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmResponse;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class RegenerateCertificateRunWriter implements ItemWriter<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegenerateCertificateRunWriter.class);

    @Value("#{stepExecutionContext['summary']}")
    AlgorithmSummaryDTO summaryDTO;

    @Override
    public void write(List<? extends Integer> list) throws Exception {
        if(!list.isEmpty()) {
            LOGGER.debug("Left:{}\n",summaryDTO.getReadCount()-summaryDTO.getProcessedCount());
        }
    }

}
