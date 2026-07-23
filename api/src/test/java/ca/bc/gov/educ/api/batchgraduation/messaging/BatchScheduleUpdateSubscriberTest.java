package ca.bc.gov.educ.api.batchgraduation.messaging;

import ca.bc.gov.educ.api.batchgraduation.service.SystemBatchSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BatchScheduleUpdateSubscriberTest {

  @Mock
  private Connection connection;

  @Mock
  private Dispatcher dispatcher;

  @Mock
  private EducGradBatchGraduationApiConstants constants;

  @Mock
  private SystemBatchSchedulingService systemBatchSchedulingService;

  @Mock
  private Message message;

  private ObjectMapper objectMapper;
  private BatchScheduleUpdateSubscriber subscriber;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    subscriber = new BatchScheduleUpdateSubscriber(connection, objectMapper, constants, systemBatchSchedulingService);
    when(constants.getBatchScheduleUpdatedSubject()).thenReturn("batch.schedule.updated");
    when(constants.getConnectionName()).thenReturn("batch-api-pod-a");
  }

  @Test
  public void testSubscribe() {
    when(connection.createDispatcher(any())).thenReturn(dispatcher);

    subscriber.subscribe();

    verify(dispatcher).subscribe("batch.schedule.updated");
  }

  @Test
  public void testHandleMessage_refreshesScheduleForOtherPodEvent() throws Exception {
    BatchScheduleUpdateEvent event = new BatchScheduleUpdateEvent();
    event.setJobType("REGALG");
    event.setOrigin("batch-api-pod-b");
    event.setUpdatedAt(LocalDateTime.now());
    when(message.getData()).thenReturn(objectMapper.writeValueAsBytes(event));

    subscriber.handleMessage(message);

    verify(systemBatchSchedulingService).refreshScheduledJob("REGALG");
  }

  @Test
  public void testHandleMessage_ignoresSelfPublishedEvent() throws Exception {
    BatchScheduleUpdateEvent event = new BatchScheduleUpdateEvent();
    event.setJobType("REGALG");
    event.setOrigin("batch-api-pod-a");
    event.setUpdatedAt(LocalDateTime.now());
    when(message.getData()).thenReturn(objectMapper.writeValueAsBytes(event));

    subscriber.handleMessage(message);

    verify(systemBatchSchedulingService, never()).refreshScheduledJob("REGALG");
  }
}
