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
public class BatchLaunchServiceTest {

    @Mock
    private Job graduationBatchJob;

    @Mock
    private Job distributionBatchJob;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private GradDashboardService gradDashboardService;

    @InjectMocks
    private BatchLaunchService batchLaunchService;

    @Test
    public void testLaunchRegularGradAlgorithm_whenEnabled_launchesBatch() throws Exception {
        BatchProcessingEntity batchProcessingEntity = new BatchProcessingEntity();
        batchProcessingEntity.setEnabled("Y");
        when(gradDashboardService.findBatchProcessing("REGALG")).thenReturn(Optional.of(batchProcessingEntity));
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(new JobExecution(210L));

        batchLaunchService.launchRegularGradAlgorithm();

        ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(any(Job.class), jobParametersCaptor.capture());
        assertThat(jobParametersCaptor.getValue().getString("jobTrigger")).isEqualTo("BATCH");
        assertThat(jobParametersCaptor.getValue().getString("jobType")).isEqualTo("REGALG");
    }

    @Test
    public void testLaunchRegularGradAlgorithm_whenDisabled_doesNotLaunchBatch() throws Exception {
        BatchProcessingEntity batchProcessingEntity = new BatchProcessingEntity();
        batchProcessingEntity.setEnabled("N");
        when(gradDashboardService.findBatchProcessing("REGALG")).thenReturn(Optional.of(batchProcessingEntity));

        batchLaunchService.launchRegularGradAlgorithm();

        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    public void testLaunchDistributionRunBatchJob_whenEnabled_launchesBatch() throws Exception {
        BatchProcessingEntity batchProcessingEntity = new BatchProcessingEntity();
        batchProcessingEntity.setEnabled("Y");
        when(gradDashboardService.findBatchProcessing("DISTRUN")).thenReturn(Optional.of(batchProcessingEntity));
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(new JobExecution(211L));

        batchLaunchService.launchDistributionRunBatchJob();

        ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(any(Job.class), jobParametersCaptor.capture());
        assertThat(jobParametersCaptor.getValue().getString("jobTrigger")).isEqualTo("BATCH");
        assertThat(jobParametersCaptor.getValue().getString("jobType")).isEqualTo("DISTRUN");
    }

    @Test
    public void testLaunchDistributionRunBatchJob_whenDisabled_doesNotLaunchBatch() throws Exception {
        BatchProcessingEntity batchProcessingEntity = new BatchProcessingEntity();
        batchProcessingEntity.setEnabled("N");
        when(gradDashboardService.findBatchProcessing("DISTRUN")).thenReturn(Optional.of(batchProcessingEntity));

        batchLaunchService.launchDistributionRunBatchJob();

        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }
}
