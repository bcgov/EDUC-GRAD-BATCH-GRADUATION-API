package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.UserScheduledJobsEntity;
import ca.bc.gov.educ.api.batchgraduation.model.JobProperName;
import ca.bc.gov.educ.api.batchgraduation.model.Task;
import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.repository.UserScheduledJobsRepository;
import ca.bc.gov.educ.api.batchgraduation.transformer.UserScheduledJobsTransformer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Service
public class TaskSchedulingService {

    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulingService.class);

    @Autowired TaskScheduler taskScheduler;
    @Autowired UserScheduledJobsRepository userScheduledJobsRepository;
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
        logger.info("Task removed {}",jobId);
    }

    public List<UserScheduledJobs> listScheduledJobs() {
        List<UserScheduledJobs> jobs = userScheduledJobsTransformer.transformToDTO(userScheduledJobsRepository.findAll());
        jobs.forEach(job-> {
            if(jobsMap.containsKey(job.getId()) && job.getStatus().equalsIgnoreCase("COMPLETED")) {
                ScheduledFuture<?> scheduledTask = jobsMap.get(job.getId());
                if(scheduledTask != null) {
                    scheduledTask.cancel(true);
                    jobsMap.remove(job.getId());
                }
            }
        });
        return jobs;
    }

    public void saveUserScheduledJobs(Task task) {
        JobProperName jName = JobProperName.valueOf(StringUtils.toRootUpperCase(task.getJobName()));
        UserScheduledJobsEntity entity = new UserScheduledJobsEntity();
        entity.setJobCode(task.getJobName());
        entity.setJobName(jName.getValue());
        entity.setCronExpression(task.getCronExpression());
        try {
            entity.setJobParameters(new ObjectMapper().writeValueAsString(task));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        entity.setStatus("QUEUED");
        entity = userScheduledJobsRepository.save(entity);
        task.setJobIdReference(entity.getId());
        task.setJobParams(entity.getJobParameters());
    }
}
