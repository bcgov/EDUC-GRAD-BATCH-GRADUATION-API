package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.JobKey;
import ca.bc.gov.educ.api.batchgraduation.model.ScheduledJobs;
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

    Map<JobKey, ScheduledFuture<?>> jobsMap = new HashMap<>();
    private Random rand = new Random();

    public void scheduleATask(String jobData, Runnable tasklet, String cronExpression) {
        String[] arrKey =   jobData.split(":");
        JobKey newJk = new JobKey();
        newJk.setJId(rand.nextInt(999999));
        newJk.setJobUser(arrKey[1]);
        newJk.setJobName(arrKey[0]);
        logger.info("Scheduling task with job id: {} and cron expression {}",newJk.getJId(),cronExpression);
        if(jobsMap.get(newJk) != null) {
            ScheduledFuture<?> taskSchd = jobsMap.get(newJk);
            if(taskSchd.isDone()){
                ScheduledFuture<?> scheduledTask = taskScheduler.schedule(tasklet, new CronTrigger(cronExpression, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
                jobsMap.put(newJk, scheduledTask);
            }
        }else {
            ScheduledFuture<?> sTask = taskScheduler.schedule(tasklet, new CronTrigger(cronExpression, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
            jobsMap.put(newJk, sTask);
        }
    }

    public void removeScheduledTask(int jobId, String jobName,String jobUser ) {
        JobKey jKey = new JobKey();
        jKey.setJobName(jobName);
        jKey.setJId(jobId);
        jKey.setJobUser(jobUser);
        ScheduledFuture<?> scheduledTask = jobsMap.get(jKey);
        if(scheduledTask != null) {
            scheduledTask.cancel(true);
            jobsMap.put(jKey, null);
        }
    }

    public List<ScheduledJobs> listScheduledJobs() {
        List<ScheduledJobs> list = new ArrayList<>();
        if(!jobsMap.isEmpty()) {
            jobsMap.forEach((k,v)->{
              ScheduledJobs sJobs = new ScheduledJobs();
              sJobs.setJobId(k.getJId());
              sJobs.setJobName(k.getJobName());
              sJobs.setScheduledBy(k.getJobUser());
              if(v.isDone()) {
                  sJobs.setStatus("Completed");
              }else {
                  sJobs.setStatus("In Queue");
              }
              list.add(sJobs);
            });
            return list;
        }
        return new ArrayList<>();
    }
}
