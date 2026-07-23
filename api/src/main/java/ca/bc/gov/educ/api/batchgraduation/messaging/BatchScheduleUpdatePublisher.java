package ca.bc.gov.educ.api.batchgraduation.messaging;

import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
@Slf4j
public class BatchScheduleUpdatePublisher {
  private final Connection connection;
  private final ObjectMapper objectMapper;
  private final EducGradBatchGraduationApiConstants constants;

  public BatchScheduleUpdatePublisher(Connection connection, ObjectMapper objectMapper, EducGradBatchGraduationApiConstants constants) {
    this.connection = connection;
    this.objectMapper = objectMapper;
    this.constants = constants;
  }

  public void publishScheduleUpdated(String jobType) {
    final BatchScheduleUpdateEvent event = new BatchScheduleUpdateEvent();
    event.setJobType(jobType);
    event.setOrigin(constants.getConnectionName());
    event.setUpdatedAt(LocalDateTime.now());

    try {
      final byte[] payload = objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
      connection.publish(constants.getBatchScheduleUpdatedSubject(), payload);
      connection.flush(Duration.ofSeconds(2));
      log.info("Published batch schedule update event for jobType={} on subject={}", jobType, constants.getBatchScheduleUpdatedSubject());
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize batch schedule update event.", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing batch schedule update event.", e);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to publish batch schedule update event.", e);
    }
  }
}
