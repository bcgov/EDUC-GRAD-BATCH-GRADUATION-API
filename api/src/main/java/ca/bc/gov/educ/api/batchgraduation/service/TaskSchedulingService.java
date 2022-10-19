package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.UserScheduledJobsEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BatchJobType;
import ca.bc.gov.educ.api.batchgraduation.model.JobProperName;
import ca.bc.gov.educ.api.batchgraduation.model.Task;
import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.repository.UserScheduledJobsRepository;
import ca.bc.gov.educ.api.batchgraduation.transformer.UserScheduledJobsTransformer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.LockableTaskScheduler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskSchedulingService {

    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulingService.class);

    @Autowired
    @Qualifier("lockableTaskScheduler")
    LockableTaskScheduler taskScheduler;

    @Autowired UserScheduledJobsRepository userScheduledJobsRepository;
    @Autowired CodeService codeService;
    @Autowired UserScheduledJobsTransformer userScheduledJobsTransformer;

    Map<UUID, ScheduledFuture<?>> jobsMap = new HashMap<>();

    public void scheduleATask(UUID jobId,Runnable tasklet, String cronExpression) {
        logger.info("Scheduled Task {}",jobId);
        ScheduledFuture<?> sTask = taskScheduler.schedule(tasklet, new CronTrigger(cronExpression, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
        jobsMap.put(jobId, sTask);
    }

    public void removeScheduledTask(UUID jobId) {
        ScheduledFuture<?> scheduledTask = jobsMap.get(jobId);
        if(scheduledTask != null) {
            scheduledTask.cancel(true);
            jobsMap.remove(jobId);
        }
        Optional<UserScheduledJobsEntity> optional = userScheduledJobsRepository.findById(jobId);
        if (optional.isPresent()) {
            userScheduledJobsRepository.delete(optional.get());
        }
        logger.info("Task removed {}",jobId);
    }

    public List<UserScheduledJobs> listScheduledJobs() {
        List<UserScheduledJobs> jobs = userScheduledJobsTransformer.transformToDTO(userScheduledJobsRepository.findAll());
        jobs.forEach(job-> checkNUpdateMap(jobsMap,job));
        return jobs;
    }

    public void checkNUpdateMap(Map<UUID, ScheduledFuture<?>> jobsMap, UserScheduledJobs job) {
        if(jobsMap.containsKey(job.getId()) && job.getStatus().equalsIgnoreCase("COMPLETED")) {
            ScheduledFuture<?> scheduledTask = jobsMap.get(job.getId());
            if(scheduledTask != null) {
                scheduledTask.cancel(true);
                jobsMap.remove(job.getId());
            }
        }
    }

    @Transactional
    public void saveUserScheduledJobs(Task task, String batchJobTypeCode) {
        JobProperName jName = JobProperName.valueOf(StringUtils.toRootUpperCase(task.getJobName()));
        String jobName = jName.getValue();
        BatchJobType jobType = codeService.getSpecificBatchJobTypeCode(batchJobTypeCode);
        if(jobType != null) {
            jobName = jobType.getLabel();
        }
        UserScheduledJobsEntity entity = new UserScheduledJobsEntity();
        entity.setJobCode(task.getJobName());
        entity.setJobName(jobName);
        entity.setCronExpression(task.getCronExpression());
        try {
            entity.setJobParameters(new ObjectMapper().writeValueAsString(task));
        } catch (JsonProcessingException e) {
            logger.debug("Error {}",e.getLocalizedMessage());
        }
        entity.setStatus("QUEUED");
        entity = userScheduledJobsRepository.save(entity);
        task.setJobIdReference(entity.getId());
        task.setJobParams(entity.getJobParameters());
    }

    @Transactional
    public void updateUserScheduledJobs(String userScheduledId) {
        Optional<UserScheduledJobsEntity> entOpt = userScheduledJobsRepository.findById(UUID.fromString(userScheduledId));
        if(entOpt.isPresent()) {
            UserScheduledJobsEntity ent = entOpt.get();
            ent.setStatus("COMPLETED");
            userScheduledJobsRepository.save(ent);
        }
    }
}
