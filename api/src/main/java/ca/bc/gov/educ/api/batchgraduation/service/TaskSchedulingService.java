package ca.bc.gov.educ.api.batchgraduation.service;

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

    Map<String, ScheduledFuture<?>> jobsMap = new HashMap<>();

    public void scheduleATask(String jobId, Runnable tasklet, String cronExpression) {
        logger.info("Scheduling task with job id: {} and cron expression {}",jobId,cronExpression);
        boolean canCreate=true;
        String[] arrKey =   jobId.split(":");
        for (Map.Entry<String, ScheduledFuture<?>> entry : jobsMap.entrySet()) {
            String jId = entry.getKey();
            if (jId.contains(cronExpression) && jId.contains(arrKey[0]) && jId.contains(arrKey[2])) {
                canCreate = false;
                break;
            }
        }
        if(jobsMap.get(jobId) != null) {
            ScheduledFuture<?> taskSchd = jobsMap.get(jobId);
            if(taskSchd.isDone()){
                ScheduledFuture<?> scheduledTask = taskScheduler.schedule(tasklet, new CronTrigger(cronExpression, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
                jobsMap.put(jobId, scheduledTask);
            }else {
                if(canCreate) {
                    ScheduledFuture<?> sTask = taskScheduler.schedule(tasklet, new CronTrigger(cronExpression, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
                    jobsMap.put(jobId, sTask);
                }
            }
        }
        ScheduledFuture<?> sTask = taskScheduler.schedule(tasklet, new CronTrigger(cronExpression, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
        jobsMap.put(jobId, sTask);
    }

    public void removeScheduledTask(String jobId) {
        ScheduledFuture<?> scheduledTask = jobsMap.get(jobId);
        if(scheduledTask != null) {
            scheduledTask.cancel(true);
            jobsMap.put(jobId, null);
        }
    }

    public List<ScheduledJobs> listScheduledJobs() {
        List<ScheduledJobs> list = new ArrayList<>();
        if(!jobsMap.isEmpty()) {
            jobsMap.forEach((k,v)->{
              String[] arrKey =   k.split(":");
              ScheduledJobs sJobs = new ScheduledJobs();
              sJobs.setJobId(k);
              sJobs.setJobName(arrKey[0]);
              sJobs.setScheduledBy(arrKey[2]);
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
