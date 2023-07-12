package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class DistributionRunYearlyNonGradWriter extends BaseYearEndWriter implements ItemWriter<List<StudentCredentialDistribution>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyNonGradWriter.class);

    @Override
    public void write(List<? extends List<StudentCredentialDistribution>> list) {
        if (!list.isEmpty()) {
            summaryDTO.increment("YED4", summaryDTO.getGlobalList().size());
            LOGGER.debug("Left:{}\n", summaryDTO.getReadCount() - summaryDTO.getProcessedCount());
        }
    }
}
