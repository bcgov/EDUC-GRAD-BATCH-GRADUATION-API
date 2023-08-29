package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

public class UserScheduledWriter implements ItemWriter<UserScheduledJobs> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserScheduledWriter.class);

    @Override
    public void write(Chunk<? extends UserScheduledJobs> list) {
        LOGGER.debug("Recording Distribution Processed Data");
        if(!list.isEmpty()) {
            LOGGER.debug("Rescheduling Done\n");
        }
    }

}
