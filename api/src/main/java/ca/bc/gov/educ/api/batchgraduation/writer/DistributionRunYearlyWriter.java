package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class DistributionRunYearlyWriter extends BaseYearEndWriter implements ItemWriter<List<StudentCredentialDistribution>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributionRunYearlyWriter.class);

    @Override
    public void write(List<? extends List<StudentCredentialDistribution>> list) throws Exception {
        if (!list.isEmpty()) {
            summaryDTO.increment("YED4");
            LOGGER.debug("Left:{}\n", summaryDTO.getReadCount() - summaryDTO.getProcessedCount());
            LOGGER.info("Starting Report Process --------------------------------------------------------------------------");
            processGlobalList(summaryDTO.getGlobalList(), jobExecutionId, summaryDTO.getMapDist(), "YEARENDDIST", restUtils.fetchAccessToken());
            LOGGER.info("=======================================================================================");
        }
    }
}
