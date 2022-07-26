package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.JobKey;
import ca.bc.gov.educ.api.batchgraduation.model.ScheduledJobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class TaskSchedulingService {

    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulingService.class);

    @Autowired TaskScheduler taskScheduler;

    Map<JobKey, ScheduledFuture<?>> jobsMap = new HashMap<>();
    private final Random rand = new SecureRandom();

    public void scheduleATask(String jobUser,String jobName, Runnable tasklet, String cronExpression) {
        logger.info("Scheduled Task {} by {}",jobName,jobUser);
        JobKey newJk = new JobKey();
        newJk.setJId(rand.nextInt(999999));
        newJk.setJobUser(jobUser);
        newJk.setJobName(jobName);
        newJk.setCronExpression(cronExpression);
        ScheduledFuture<?> sTask = taskScheduler.schedule(tasklet, new CronTrigger(cronExpression, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
        jobsMap.put(newJk, sTask);
    }

    public void removeScheduledTask(int jobId, String jobName,String jobUser) {
        JobKey jKey = new JobKey();
        jKey.setJobName(jobName);
        jKey.setJId(jobId);
        jKey.setJobUser(jobUser);
        ScheduledFuture<?> scheduledTask = jobsMap.get(jKey);
        if(scheduledTask != null) {
            scheduledTask.cancel(true);
            jobsMap.remove(jKey);
        }
        logger.info("Task removed {}",jobId);
    }

    public List<ScheduledJobs> listScheduledJobs() {
        List<ScheduledJobs> list = new ArrayList<>();
        if(!jobsMap.isEmpty()) {
            jobsMap.forEach((k,v)->{
              ScheduledJobs sJobs = new ScheduledJobs();
              sJobs.setRowId(k.getJId()+"_"+k.getJobName()+"_"+k.getJobUser());
              sJobs.setJobId(k.getJId());
              sJobs.setJobName(k.getJobName());
              sJobs.setScheduledBy(k.getJobUser());
              sJobs.setCronExpression(k.getCronExpression());
              if(v != null) {
                  if (v.getDelay(TimeUnit.MINUTES) < 0) {
                      sJobs.setStatus("Completed");
                  } else {
                      sJobs.setStatus("In Queue");
                  }
              }
              list.add(sJobs);
            });
            return list;
        }
        return new ArrayList<>();
    }
}
