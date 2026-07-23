package ca.bc.gov.educ.api.batchgraduation.messaging;

import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BatchScheduleUpdatePublisherTest {

  @Mock
  private Connection connection;

  @Mock
  private EducGradBatchGraduationApiConstants constants;

  private ObjectMapper objectMapper;
  private BatchScheduleUpdatePublisher publisher;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    publisher = new BatchScheduleUpdatePublisher(connection, objectMapper, constants);
    when(constants.getBatchScheduleUpdatedSubject()).thenReturn("batch.schedule.updated");
    when(constants.getConnectionName()).thenReturn("batch-api-pod-a");
  }

  @Test
  public void testPublishScheduleUpdated() throws Exception {
    publisher.publishScheduleUpdated("REGALG");

    ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(connection).publish(eq("batch.schedule.updated"), payloadCaptor.capture());
    verify(connection).flush(any(java.time.Duration.class));

    BatchScheduleUpdateEvent event = objectMapper.readValue(
        new String(payloadCaptor.getValue(), StandardCharsets.UTF_8),
        BatchScheduleUpdateEvent.class
    );

    assertThat(event.getJobType()).isEqualTo("REGALG");
    assertThat(event.getOrigin()).isEqualTo("batch-api-pod-a");
    assertThat(event.getUpdatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  public void testPublishScheduleUpdated_whenInterrupted_restoresInterruptAndThrows() throws Exception {
    doThrow(new InterruptedException("interrupted")).when(connection).flush(any(java.time.Duration.class));

    try {
      publisher.publishScheduleUpdated("REGALG");
      fail("Expected IllegalStateException");
    } catch (IllegalStateException ex) {
      assertThat(ex.getMessage()).isEqualTo("Interrupted while publishing batch schedule update event.");
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      Thread.interrupted();
    }
  }

  @Test
  public void testPublishScheduleUpdated_whenPublishFails_throws() {
    doThrow(new RuntimeException("publish failed")).when(connection).publish(eq("batch.schedule.updated"), any(byte[].class));

    try {
      publisher.publishScheduleUpdated("REGALG");
      fail("Expected IllegalStateException");
    } catch (IllegalStateException ex) {
      assertThat(ex.getMessage()).isEqualTo("Failed to publish batch schedule update event.");
    }
  }
}
