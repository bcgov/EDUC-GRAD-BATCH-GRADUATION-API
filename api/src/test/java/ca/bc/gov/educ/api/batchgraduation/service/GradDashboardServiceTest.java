package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
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

}
