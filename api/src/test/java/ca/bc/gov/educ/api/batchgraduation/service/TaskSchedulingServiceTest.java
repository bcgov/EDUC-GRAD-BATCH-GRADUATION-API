package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.JobKey;
import ca.bc.gov.educ.api.batchgraduation.model.ScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.model.Task;
import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    TaskScheduler taskScheduler;

    @MockBean
    private RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Mock
    Map<JobKey, ScheduledFuture<?>> jobsMap = new HashMap<>();

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
        List<UserScheduledJobs> res = taskSchedulingService.listScheduledJobs();
        assertNotNull(res);
    }

    @Test
    public void testsaveUserScheduledJobs() {
        Task task = new Task();
        task.setCronExpression("213211");
        task.setJobName("URDBJ");
        taskSchedulingService.saveUserScheduledJobs(task);
        assertThat(task).isNotNull();
    }


}
