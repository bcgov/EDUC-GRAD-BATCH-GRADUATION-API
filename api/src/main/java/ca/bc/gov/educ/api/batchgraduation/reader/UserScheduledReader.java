package ca.bc.gov.educ.api.batchgraduation.reader;

import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.repository.UserScheduledJobsRepository;
import ca.bc.gov.educ.api.batchgraduation.transformer.UserScheduledJobsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class UserScheduledReader implements ItemReader<UserScheduledJobs> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserScheduledReader.class);

    @Autowired
    UserScheduledJobsRepository userScheduledJobsRepository;
    @Autowired
    UserScheduledJobsTransformer userScheduledJobsTransformer;

    private int nextJobForProcessing;
    List<UserScheduledJobs> jobList;

    @Override
    public UserScheduledJobs read() throws Exception {
        LOGGER.info("Reading next Credential");

        if (jobDataIsNotInitialized()) {
            jobList = findJobList();
            LOGGER.info("Found {} records",jobList.size());
        }

        UserScheduledJobs nextJob = null;

        
        if (nextJobForProcessing < jobList.size()) {
            nextJob = jobList.get(nextJobForProcessing);
            LOGGER.info("Job ID:{} - {} of {}", nextJob.getId(), nextJobForProcessing + 1, jobList.size());
            nextJobForProcessing++;
        }else {
            nextJobForProcessing = 0;
            jobList = null;
        }
        return nextJob;
    }

    private boolean jobDataIsNotInitialized() {
        return this.jobList == null;
    }

    private List<UserScheduledJobs> findJobList() {
        return userScheduledJobsTransformer.transformToDTO(userScheduledJobsRepository.findByStatus("QUEUED"));
    }
}
