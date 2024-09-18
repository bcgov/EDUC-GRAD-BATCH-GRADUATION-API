package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.UserScheduledJobsEntity;
import ca.bc.gov.educ.api.batchgraduation.model.Task;
import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.repository.UserScheduledJobsRepository;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import net.javacrumbs.shedlock.spring.LockableTaskScheduler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class TaskSchedulingServiceTest {

    @Autowired
    TaskSchedulingService taskSchedulingService;

    @Autowired
    TaskDefinition taskDefinition;

    @MockBean
    @Qualifier("lockableTaskScheduler")
    LockableTaskScheduler taskScheduler;

    @Autowired
    JsonTransformer jsonTransformer;

    @MockBean
    private UserScheduledJobsRepository userScheduledJobsRepository;

    @Mock
    private CodeService codeService;


    @Test
    public void testScheduleATask() {
        String jobUser = "John Doe";
        String jobName="SGBJ";
        Task task = new Task();
        task.setJobName(jobName);
        task.setJobIdReference(UUID.randomUUID());
        taskSchedulingService.scheduleATask(task.getJobIdReference(),taskDefinition,"0 5 20 * * *");
        assertThat(jobName).isEqualTo("SGBJ");
    }

    @Test
    public void testRemoveScheduledTask() {
        UUID jobId= UUID.randomUUID();
        String jobName="SGBJ";
        taskSchedulingService.removeScheduledTask(jobId);
        assertThat(jobName).isEqualTo("SGBJ");
    }

    @Test
    public void testListScheduledJobs() {
        UUID jobId = UUID.randomUUID();

        UserScheduledJobsEntity jobs = new UserScheduledJobsEntity();
        jobs.setId(jobId);
        jobs.setJobName("Blah");
        jobs.setJobCode("BKSS");
        jobs.setStatus("COMPLETED");
        Mockito.when(userScheduledJobsRepository.findAll()).thenReturn(List.of(jobs));

        List<UserScheduledJobs> res = taskSchedulingService.listScheduledJobs();
        assertNotNull(res);
    }

    @Test
    public void testcheckNUpdateMap() {
        UUID jobId = UUID.randomUUID();

        UserScheduledJobs jobs = new UserScheduledJobs();
        jobs.setId(jobId);
        jobs.setJobName("Blah");
        jobs.setJobCode("BKSS");
        jobs.setStatus("COMPLETED");

        Map<UUID, ScheduledFuture<?>> jobsMap = new HashMap<>();
        ScheduledFuture<?> sTask = taskScheduler.schedule(taskDefinition, new CronTrigger("0 12 23 5 7 *", TimeZone.getTimeZone(TimeZone.getDefault().getID())));
        jobsMap.put(jobId,sTask);
        taskSchedulingService.checkNUpdateMap(jobsMap,jobs);
        assertThat(jobs).isNotNull();
    }

   /* @Test
    public void testsaveUserScheduledJobs() {
        Task task = new Task();
        task.setCronExpression("213211");
        task.setJobName("URDBJ");

        UserScheduledJobsEntity entity = new UserScheduledJobsEntity();
        entity.setJobCode(task.getJobName());
        entity.setJobName("User Req Distribution Batch Job");
        entity.setCronExpression(task.getCronExpression());
        entity.setJobParameters(jsonTransformer.marshall(task));

        entity.setStatus("QUEUED");

        UserScheduledJobsEntity ent2 = new UserScheduledJobsEntity();
        ent2.setId(UUID.randomUUID());
        ent2.setJobName("FREE");
        ent2.setJobParameters("Adsad");

        String batchJobType = "TVRRUN";

        BatchJobType obj = new BatchJobType();
        obj.setCode(batchJobType);
        obj.setDescription("Student Achievement Report (TVR)");
        obj.setCreateUser("GRADUATION");
        obj.setUpdateUser("GRADUATION");
        obj.setCreateDate(LocalDateTime.now());
        obj.setUpdateDate(LocalDateTime.now());
        Mockito.when(codeService.getSpecificBatchJobTypeCode(batchJobType)).thenReturn(obj);

        Mockito.when(userScheduledJobsRepository.save(entity)).thenReturn(ent2);
        taskSchedulingService.saveUserScheduledJobs(task, batchJobType);
        assertThat(task).isNotNull();
    }*/


}
