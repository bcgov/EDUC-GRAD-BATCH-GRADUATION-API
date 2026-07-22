package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TvrLaunchServiceTest {

    @Mock
    private Job tvrBatchJob;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private GradDashboardService gradDashboardService;

    @InjectMocks
    private TvrLaunchService tvrLaunchService;

    @Test
    public void testLaunchTVRReportProcess_whenEnabled_launchesBatch() throws Exception {
        BatchProcessingEntity batchProcessingEntity = new BatchProcessingEntity();
        batchProcessingEntity.setEnabled("Y");
        when(gradDashboardService.findBatchProcessing("TVRRUN")).thenReturn(Optional.of(batchProcessingEntity));
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(new JobExecution(210L));

        tvrLaunchService.launchTVRReportProcess();

        ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(any(Job.class), jobParametersCaptor.capture());
        assertThat(jobParametersCaptor.getValue().getString("jobTrigger")).isEqualTo("BATCH");
        assertThat(jobParametersCaptor.getValue().getString("jobType")).isEqualTo("TVRRUN");
    }

    @Test
    public void testLaunchTVRReportProcess_whenDisabled_doesNotLaunchBatch() throws Exception {
        BatchProcessingEntity batchProcessingEntity = new BatchProcessingEntity();
        batchProcessingEntity.setEnabled("N");
        when(gradDashboardService.findBatchProcessing("TVRRUN")).thenReturn(Optional.of(batchProcessingEntity));

        tvrLaunchService.launchTVRReportProcess();

        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }
}
