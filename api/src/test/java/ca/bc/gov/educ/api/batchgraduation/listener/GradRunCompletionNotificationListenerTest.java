package ca.bc.gov.educ.api.batchgraduation.listener;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.AlgorithmSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.service.TvrLaunchService;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GradRunCompletionNotificationListenerTest {

    private static final String TIME = "time";
    private static final String JOB_TRIGGER = "jobTrigger";
    private static final String JOB_TYPE = "jobType";

    @Autowired
    private GradRunCompletionNotificationListener gradRunCompletionNotificationListener;

    @MockBean
    private GradBatchHistoryService gradBatchHistoryService;

    @MockBean
    private TaskSchedulingService taskSchedulingService;

    @MockBean
    private RestUtils restUtils;

    @MockBean
    private JsonTransformer jsonTransformer;

    @MockBean
    private TvrLaunchService tvrLaunchService;

    @Test
    public void testAfterJob_whenScheduledBatch_completes_launchesTvr() {
        JobExecution jobExecution = createJobExecution("BATCH", BatchStatus.COMPLETED);

        gradRunCompletionNotificationListener.afterJob(jobExecution);

        verify(tvrLaunchService).launchTVRReportProcess();
    }

    @Test
    public void testAfterJob_whenManualBatch_completes_doesNotLaunchTvr() {
        JobExecution jobExecution = createJobExecution("MANUAL", BatchStatus.COMPLETED);

        gradRunCompletionNotificationListener.afterJob(jobExecution);

        verify(tvrLaunchService, never()).launchTVRReportProcess();
    }

    @Test
    public void testAfterJob_whenBatchNotCompleted_doesNotLaunchTvr() {
        JobExecution jobExecution = createJobExecution("BATCH", BatchStatus.FAILED);

        gradRunCompletionNotificationListener.afterJob(jobExecution);

        verify(tvrLaunchService, never()).launchTVRReportProcess();
    }

    private JobExecution createJobExecution(String trigger, BatchStatus status) {
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis());
        builder.addString(JOB_TRIGGER, trigger);
        builder.addString(JOB_TYPE, "REGALG");

        JobExecution jobExecution = new JobExecution(
                new JobInstance(1L, "GraduationBatchJob"),
                121L,
                builder.toJobParameters()
        );
        jobExecution.setStatus(status);
        jobExecution.setStartTime(LocalDateTime.now());
        jobExecution.setEndTime(LocalDateTime.now());

        ExecutionContext jobContext = new ExecutionContext();
        jobContext.put("regGradAlgSummaryDTO", new AlgorithmSummaryDTO());
        jobExecution.setExecutionContext(jobContext);

        when(jsonTransformer.unmarshall("{}", StudentSearchRequest.class)).thenReturn(new StudentSearchRequest());
        when(gradBatchHistoryService.getCountForReadStudent(121L)).thenReturn(0L);
        when(gradBatchHistoryService.getCountForProcessedStudent(121L)).thenReturn(0L);
        when(gradBatchHistoryService.getCountForErroredStudent(121L)).thenReturn(0L);
        when(gradBatchHistoryService.getSchoolListForReport(121L)).thenReturn(Collections.emptyList());
        when(gradBatchHistoryService.saveGradAlgorithmJobHistory(Mockito.any(BatchGradAlgorithmJobHistoryEntity.class)))
                .thenReturn(new BatchGradAlgorithmJobHistoryEntity());

        return jobExecution;
    }
}
