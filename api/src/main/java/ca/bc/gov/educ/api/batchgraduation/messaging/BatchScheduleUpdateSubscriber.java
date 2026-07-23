package ca.bc.gov.educ.api.batchgraduation.messaging;

import ca.bc.gov.educ.api.batchgraduation.service.SystemBatchSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class BatchScheduleUpdateSubscriber {
  private final Connection connection;
  private final ObjectMapper objectMapper;
  private final EducGradBatchGraduationApiConstants constants;
  private final SystemBatchSchedulingService systemBatchSchedulingService;
  private Dispatcher dispatcher;

  public BatchScheduleUpdateSubscriber(Connection connection,
                                       ObjectMapper objectMapper,
                                       EducGradBatchGraduationApiConstants constants,
                                       SystemBatchSchedulingService systemBatchSchedulingService) {
    this.connection = connection;
    this.objectMapper = objectMapper;
    this.constants = constants;
    this.systemBatchSchedulingService = systemBatchSchedulingService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void subscribe() {
    if (dispatcher != null) {
      return;
    }
    dispatcher = connection.createDispatcher(this::handleMessage);
    dispatcher.subscribe(constants.getBatchScheduleUpdatedSubject());
    log.info("Subscribed to NATS subject {}", constants.getBatchScheduleUpdatedSubject());
  }

  @PreDestroy
  public void shutdown() {
    if (dispatcher != null) {
      dispatcher.unsubscribe(constants.getBatchScheduleUpdatedSubject());
      dispatcher = null;
    }
  }

  void handleMessage(Message message) {
    try {
      BatchScheduleUpdateEvent event = objectMapper.readValue(message.getData(), BatchScheduleUpdateEvent.class);
      log.info("Received batch schedule update event for jobType={}", event.getJobType());
      systemBatchSchedulingService.refreshScheduledJob(event.getJobType());
    } catch (IOException e) {
      log.error("Failed to deserialize batch schedule update event payload {}", new String(message.getData(), StandardCharsets.UTF_8), e);
    } catch (RuntimeException e) {
      log.error("Failed to refresh scheduled job from batch schedule update event.", e);
    }
  }
}
