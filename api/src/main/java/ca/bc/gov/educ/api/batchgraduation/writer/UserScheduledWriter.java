package ca.bc.gov.educ.api.batchgraduation.writer;

import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class UserScheduledWriter implements ItemWriter<UserScheduledJobs> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserScheduledWriter.class);

    @Override
    public void write(List<? extends UserScheduledJobs> list) throws Exception {
        LOGGER.info("Recording Distribution Processed Data");
        if(!list.isEmpty()) {
            LOGGER.info("Rescheduling Done\n");
        }
    }

}
