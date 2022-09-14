package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessing;
import ca.bc.gov.educ.api.batchgraduation.model.ScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.model.Task;
import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import ca.bc.gov.educ.api.batchgraduation.service.TaskDefinition;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SchedulingControllerTest {

    @Mock
    TaskSchedulingService taskSchedulingService;

    @Mock
    GradDashboardService gradDashboardService;

    @Mock
    private TaskDefinition taskDefinition;

    @Mock
    private JobRegistry jobRegistry;

    @Mock
    private JobParametersBuilder jobParametersBuilder;

    @InjectMocks
    private SchedulingController schedulingController;

    @MockBean
    WebClient webClient;

    @MockBean
    RestUtils restUtils;

    @Test
    public void testscheduleATask() {
        Task task = new Task();
        task.setCredentialType("OT");
        task.setCronExpression("0 34 4 5 6 *");
        task.setJobName("SRBJ");
        UUID jobId = UUID.randomUUID();
        task.setJobIdReference(jobId);
        Mockito.doNothing().when(taskSchedulingService).scheduleATask(jobId,taskDefinition,"0 34 4 5 6 *");
        schedulingController.scheduleATask(task, "TVRRUN");
        Mockito.verify(taskSchedulingService).scheduleATask(jobId,taskDefinition,"0 34 4 5 6 *");
    }
    @Test
    public void testscheduleATask_2() {
        Task task = new Task();
        task.setCredentialType("OT");
        task.setCronExpression("0 34 4 5 6 *");
        task.setJobName("SRBJ");
        task.setDeliveredToUser(true);
        UUID jobId = UUID.randomUUID();
        task.setJobIdReference(jobId);
        Mockito.doNothing().when(taskSchedulingService).scheduleATask(jobId,taskDefinition,"0 34 4 5 6 *");
        schedulingController.scheduleATask(task, "TVRRUN");
        Mockito.verify(taskSchedulingService).scheduleATask(jobId,taskDefinition,"0 34 4 5 6 *");
    }

    @Test
    public void testRemoveJob() {
        UUID jobId = UUID.randomUUID();
        Mockito.doNothing().when(taskSchedulingService).removeScheduledTask(jobId);
        schedulingController.removeJob(jobId.toString());
        Mockito.verify(taskSchedulingService).removeScheduledTask(jobId);
    }

    @Test
    public void testListJobs() {
        UserScheduledJobs sJobs = new UserScheduledJobs();
        sJobs.setStatus("COMPLETED");
        sJobs.setCreateUser("SKS");
        sJobs.setJobName("SRBJ");
        sJobs.setId(UUID.randomUUID());
        sJobs.setCronExpression("0 34 4 5 6 *");
        Mockito.when(taskSchedulingService.listScheduledJobs()).thenReturn(List.of(sJobs));
        ResponseEntity<List<UserScheduledJobs>> res = schedulingController.listJobs();
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    public void testListJobs_empty() {
        ScheduledJobs sJobs = new ScheduledJobs();
        sJobs.setStatus("COMPLETED");
        sJobs.setScheduledBy("SKS");
        sJobs.setJobName("SRBJ");
        sJobs.setRowId("1231123_SRBJ_SKS");
        sJobs.setCronExpression("0 34 4 5 6 *");
        Mockito.when(taskSchedulingService.listScheduledJobs()).thenReturn(new ArrayList<>());
        ResponseEntity<List<UserScheduledJobs>> res = schedulingController.listJobs();
        assertThat(res.getBody()).isNull();
    }

    @Test
    public void testprocessingList() {
        BatchProcessing sJobs = new BatchProcessing();
        sJobs.setJobType("PSIRUN");
        sJobs.setId(UUID.randomUUID());
        sJobs.setEnabled("Y");
        sJobs.setCronExpression("0 34 4 5 6 *");
        Mockito.when(gradDashboardService.getProcessingList()).thenReturn(List.of(sJobs));
        ResponseEntity<List<BatchProcessing>> res = schedulingController.processingList();
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    public void testprocessingList_empty() {
        Mockito.when(taskSchedulingService.listScheduledJobs()).thenReturn(new ArrayList<>());
        ResponseEntity<List<UserScheduledJobs>> res = schedulingController.listJobs();
        assertThat(res.getBody()).isNull();
    }

    @Test
    public void testprocessingList_2() {
        BatchProcessing sJobs = new BatchProcessing();
        String jobType = "PSIRUN";
        sJobs.setJobType(jobType);
        sJobs.setId(UUID.randomUUID());
        sJobs.setEnabled("Y");
        sJobs.setCronExpression("0 34 4 5 6 *");
        Mockito.when(gradDashboardService.toggleProcess(jobType)).thenReturn(sJobs);
        ResponseEntity<BatchProcessing> res = schedulingController.processingList(jobType);
        assertThat(res.getBody()).isNotNull();
    }

    @Test
    public void testprocessingList_3() {
        Mockito.when(gradDashboardService.toggleProcess("TVRRUN")).thenReturn(null);
        ResponseEntity<BatchProcessing> res = schedulingController.processingList("TVRRUN");
        assertThat(res.getBody()).isNull();
    }
}
