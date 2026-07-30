package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.messaging.BatchScheduleUpdatePublisher;
import ca.bc.gov.educ.api.batchgraduation.model.BatchPipelineStatus;
import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessingSchedule;
import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessingScheduleUpdateRequest;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchProcessingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BatchProcessingScheduleServiceTest {

    @Mock
    private BatchProcessingRepository batchProcessingRepository;

    @Mock
    private SystemBatchSchedulingService systemBatchSchedulingService;

    @Mock
    private GradBatchHistoryService gradBatchHistoryService;

    @Mock
    private BatchScheduleUpdatePublisher batchScheduleUpdatePublisher;

    @InjectMocks
    private BatchProcessingScheduleService batchProcessingScheduleService;

    private BatchProcessingEntity regalgSchedule;

    @Before
    public void setUp() {
        regalgSchedule = new BatchProcessingEntity();
        regalgSchedule.setJobType("REGALG");
        regalgSchedule.setEnabled("Y");
        regalgSchedule.setCronExpression("0 30 18 * * *");
    }

    @Test
    public void testGetBatchProcessingSchedule() {
        when(batchProcessingRepository.findByJobType("REGALG")).thenReturn(Optional.of(regalgSchedule));

        BatchProcessingSchedule response = batchProcessingScheduleService.getBatchProcessingSchedule("REGALG");

        assertThat(response.getJobType()).isEqualTo("REGALG");
        assertThat(response.getStartTime()).isEqualTo("18:30");
        assertThat(response.getCronExpression()).isEqualTo("0 30 18 * * *");
    }

    @Test
    public void testUpdateBatchProcessingSchedule() {
        String today = LocalDate.now(ZoneId.systemDefault()).toString();
        BatchProcessingScheduleUpdateRequest request = new BatchProcessingScheduleUpdateRequest();
        request.setScheduledDateTime(today + "T23:00:00");
        request.setTimeZone(ZoneId.systemDefault().getId());

        BatchPipelineStatus pipelineStatus = new BatchPipelineStatus();
        pipelineStatus.setRunning(false);
        when(batchProcessingRepository.findByJobType("REGALG")).thenReturn(Optional.of(regalgSchedule));
        when(gradBatchHistoryService.getBatchPipelineStatus()).thenReturn(pipelineStatus);
        when(batchProcessingRepository.save(any(BatchProcessingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BatchProcessingSchedule response = batchProcessingScheduleService.updateBatchProcessingSchedule("REGALG", request);

        assertThat(response.getCronExpression()).isEqualTo("0 0 23 * * *");
        assertThat(response.getStartTime()).isEqualTo("23:00");
        verify(systemBatchSchedulingService).refreshScheduledJob("REGALG");
        verify(batchScheduleUpdatePublisher).publishScheduleUpdated("REGALG");
    }

    @Test
    public void testUpdateBatchProcessingSchedule_whenDateIsNotToday_throwsValidationError() {
        BatchProcessingScheduleUpdateRequest request = new BatchProcessingScheduleUpdateRequest();
        request.setScheduledDateTime(LocalDate.now(ZoneId.systemDefault()).plusDays(1) + "T10:00:00");
        request.setTimeZone(ZoneId.systemDefault().getId());

        BatchPipelineStatus pipelineStatus = new BatchPipelineStatus();
        pipelineStatus.setRunning(false);
        when(batchProcessingRepository.findByJobType("REGALG")).thenReturn(Optional.of(regalgSchedule));
        when(gradBatchHistoryService.getBatchPipelineStatus()).thenReturn(pipelineStatus);

        try {
            batchProcessingScheduleService.updateBatchProcessingSchedule("REGALG", request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).contains("Scheduled date must be today");
        }
    }

    @Test
    public void testUpdateBatchProcessingSchedule_whenTimeIsInPast_throwsValidationError() {
        LocalDateTime oneMinuteAgo = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        BatchProcessingScheduleUpdateRequest request = new BatchProcessingScheduleUpdateRequest();
        request.setScheduledDateTime(oneMinuteAgo.withSecond(0).withNano(0).toString());
        request.setTimeZone(ZoneId.systemDefault().getId());

        BatchPipelineStatus pipelineStatus = new BatchPipelineStatus();
        pipelineStatus.setRunning(false);
        when(batchProcessingRepository.findByJobType("REGALG")).thenReturn(Optional.of(regalgSchedule));
        when(gradBatchHistoryService.getBatchPipelineStatus()).thenReturn(pipelineStatus);

        try {
            batchProcessingScheduleService.updateBatchProcessingSchedule("REGALG", request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("Scheduled time must be later than the current time.");
        }
    }

    @Test
    public void testUpdateBatchProcessingSchedule_whenPipelineIsRunning_throwsValidationError() {
        String today = LocalDate.now(ZoneId.systemDefault()).toString();
        BatchProcessingScheduleUpdateRequest request = new BatchProcessingScheduleUpdateRequest();
        request.setScheduledDateTime(today + "T23:00:00");
        request.setTimeZone(ZoneId.systemDefault().getId());

        BatchPipelineStatus pipelineStatus = new BatchPipelineStatus();
        pipelineStatus.setRunning(true);
        when(gradBatchHistoryService.getBatchPipelineStatus()).thenReturn(pipelineStatus);

        try {
            batchProcessingScheduleService.updateBatchProcessingSchedule("REGALG", request);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("Cannot update the REGALG schedule while a REGALG or TVRRUN batch is running.");
        }
    }
}
