package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchProcessingRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GradDashboardServiceTest {

    @Autowired
    GradDashboardService gradDashboardService;

    @MockBean
    BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;

    @MockBean
    BatchProcessingRepository batchProcessingRepository;

    @MockBean
    private RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Test
    public void testGetDashboardInfo() {

        List<BatchGradAlgorithmJobHistoryEntity> list = new ArrayList<>();
        BatchGradAlgorithmJobHistoryEntity hist = new BatchGradAlgorithmJobHistoryEntity();
        hist.setEndTime(new Date());
        hist.setActualStudentsProcessed(11L);
        hist.setId(new UUID(1,1));
        hist.setExpectedStudentsProcessed(20L);
        hist.setJobExecutionId(121L);
        hist.setFailedStudentsProcessed(4);
        hist.setStartTime(new Date());
        list.add(hist);
        when(batchGradAlgorithmJobHistoryRepository.findAll()).thenReturn(list);
        GradDashboard dash = gradDashboardService.getDashboardInfo();
        assertThat(dash).isNotNull();
        assertThat(dash.getTotalBatchRuns()).isEqualTo(1);

    }

    @Test
    public void testgetProcessingList() {

        List<BatchProcessingEntity> list = new ArrayList<>();
        BatchProcessingEntity hist = new BatchProcessingEntity();
        hist.setEnabled("Y");
        hist.setJobType("REGALG");
        hist.setScheduleOccurrence("D");
        list.add(hist);
        when(batchProcessingRepository.findAll()).thenReturn(list);
        List<BatchProcessing> dash = gradDashboardService.getProcessingList();
        assertThat(dash).isNotNull();
        assertThat(dash.get(0).getEnabled()).isEqualTo("Y");

    }

    @Test
    public void testUpdateProcessing() {
        UUID processingId = UUID.randomUUID();
        BatchProcessingEntity hist = new BatchProcessingEntity();
        hist.setEnabled("Y");
        hist.setJobType("REGALG");
        hist.setScheduleOccurrence("D");

        BatchProcessingEntity hist2 = new BatchProcessingEntity();
        hist2.setEnabled("N");
        hist2.setJobType("REGALG");
        hist2.setScheduleOccurrence("D");
        when(batchProcessingRepository.findById(processingId)).thenReturn(Optional.of(hist));
        when(batchProcessingRepository.save(hist2)).thenReturn(hist2);
        BatchProcessing dash = gradDashboardService.toggleProcess(processingId);
        assertThat(dash).isNotNull();
        assertThat(dash.getEnabled()).isEqualTo("N");

    }

}
