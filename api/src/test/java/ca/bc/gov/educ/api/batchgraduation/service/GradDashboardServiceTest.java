package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.*;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
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
    BatchGradAlgorithmStudentRepository batchGradAlgorithmStudentRepository;

    @MockBean
    BatchJobExecutionRepository batchJobExecutionRepository;

    @MockBean
    BatchStepExecutionRepository batchStepExecutionRepository;

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
        hist.setEndTime(LocalDateTime.now());
        hist.setActualStudentsProcessed(11L);
        hist.setId(new UUID(1,1));
        hist.setExpectedStudentsProcessed(20L);
        hist.setJobExecutionId(121L);
        hist.setFailedStudentsProcessed(4);
        hist.setStartTime(LocalDateTime.now());
        list.add(hist);
        when(batchGradAlgorithmJobHistoryRepository.findAll()).thenReturn(list);
        GradDashboard dash = gradDashboardService.getDashboardInfo();
        assertThat(dash).isNotNull();
        assertThat(dash.getTotalBatchRuns()).isEqualTo(1);

    }

    @Test
    public void testGetDashboardInfo_whenStartedDate_isOlderThan3Days_thenUpdateStatusAsFailed() {

        List<BatchGradAlgorithmJobHistoryEntity> list = new ArrayList<>();
        BatchGradAlgorithmJobHistoryEntity hist = new BatchGradAlgorithmJobHistoryEntity();
        hist.setId(new UUID(1,1));
        hist.setExpectedStudentsProcessed(20L);
        hist.setJobExecutionId(121L);
        Date today = new Date(System.currentTimeMillis());
        Date startedDateTime = DateUtils.addDays(today, -4);
        hist.setStartTime(ca.bc.gov.educ.api.batchgraduation.util.DateUtils.toLocalDateTime(startedDateTime));
        hist.setStatus("STARTED");
        list.add(hist);

        BatchJobExecutionEntity batchJobExecution = new BatchJobExecutionEntity();
        batchJobExecution.setJobExecutionId(hist.getJobExecutionId());
        batchJobExecution.setId(Long.valueOf("123"));
        batchJobExecution.setStatus("STARTED");
        batchJobExecution.setExitCode("UNKNOWN");
        batchJobExecution.setStartTime(hist.getStartTime());

        when(batchGradAlgorithmJobHistoryRepository.findAll()).thenReturn(list);
        when(batchJobExecutionRepository.findById(hist.getJobExecutionId())).thenReturn(Optional.of(batchJobExecution));

        GradDashboard dash = gradDashboardService.getDashboardInfo();
        assertThat(dash).isNotNull();
        assertThat(dash.getTotalBatchRuns()).isEqualTo(1);
        assertThat(dash.getBatchInfoList()).isNotEmpty();
        assertThat(dash.getBatchInfoList().get(0).getStatus()).isEqualTo("FAILED");

    }

    @Test
    public void testGetDashboardInfo_whenLastUpdatedDate_isOlderThan5hours_thenUpdateStatusAsFailed() {

        List<BatchGradAlgorithmJobHistoryEntity> list = new ArrayList<>();
        BatchGradAlgorithmJobHistoryEntity hist = new BatchGradAlgorithmJobHistoryEntity();
        hist.setId(new UUID(1,1));
        hist.setExpectedStudentsProcessed(20L);
        hist.setJobExecutionId(121L);
        Date today = new Date(System.currentTimeMillis());
        Date startedDateTime = DateUtils.addDays(today, -1);
        hist.setStartTime(ca.bc.gov.educ.api.batchgraduation.util.DateUtils.toLocalDateTime(startedDateTime));
        hist.setStatus("STARTED");
        list.add(hist);

        BatchJobExecutionEntity batchJobExecution = new BatchJobExecutionEntity();
        batchJobExecution.setJobExecutionId(hist.getJobExecutionId());
        batchJobExecution.setId(Long.valueOf("123"));
        batchJobExecution.setStatus("STARTED");
        batchJobExecution.setStartTime(hist.getStartTime());

        BatchStepExecutionEntity step = new BatchStepExecutionEntity();
        step.setStepName("test-partition12");
        step.setJobExecutionId(hist.getJobExecutionId());
        step.setStepExecutionId(Long.valueOf("123"));
        step.setStatus("STARTED");
        step.setStartTime(ca.bc.gov.educ.api.batchgraduation.util.DateUtils.toLocalDateTime(startedDateTime));
        Date lastUpdatedDateTime = DateUtils.addHours(today, -6);
        step.setLastUpdated(ca.bc.gov.educ.api.batchgraduation.util.DateUtils.toLocalDateTime(lastUpdatedDateTime));
        batchJobExecution.setStartTime(hist.getStartTime());

        when(batchGradAlgorithmJobHistoryRepository.findAll()).thenReturn(list);
        when(batchJobExecutionRepository.findById(hist.getJobExecutionId())).thenReturn(Optional.of(batchJobExecution));
        when(batchStepExecutionRepository.findByJobExecutionIdOrderByEndTimeDesc(hist.getJobExecutionId())).thenReturn(List.of(step));

        GradDashboard dash = gradDashboardService.getDashboardInfo();
        assertThat(dash).isNotNull();
        assertThat(dash.getTotalBatchRuns()).isEqualTo(1);
        assertThat(dash.getBatchInfoList()).isNotEmpty();
        assertThat(dash.getBatchInfoList().get(0).getStatus()).isEqualTo("FAILED");

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
        String jobType = "REGALG";
        BatchProcessingEntity hist = new BatchProcessingEntity();
        hist.setEnabled("Y");
        hist.setJobType(jobType);
        hist.setScheduleOccurrence("D");

        BatchProcessingEntity hist2 = new BatchProcessingEntity();
        hist2.setEnabled("N");
        hist2.setJobType(jobType);
        hist2.setScheduleOccurrence("D");
        when(batchProcessingRepository.findByJobType(jobType)).thenReturn(Optional.of(hist));
        when(batchProcessingRepository.save(hist2)).thenReturn(hist2);
        BatchProcessing dash = gradDashboardService.toggleProcess(jobType);
        assertThat(dash).isNotNull();
        assertThat(dash.getEnabled()).isEqualTo("N");

    }

    @Test
    public void testGetErrorInfo() {
        Pageable paging = PageRequest.of(0, 10);
        Long batchId= 123123L;
        UUID studentId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity ent = new BatchGradAlgorithmStudentEntity();
        ent.setError("weqw");
        ent.setJobExecutionId(batchId);
        ent.setId(UUID.randomUUID());
        ent.setStudentID(studentId);
        Page<BatchGradAlgorithmStudentEntity> pagedData = new PageImpl(List.of(ent));

        GraduationStudentRecord rec = new GraduationStudentRecord();
        rec.setStudentID(studentId);
        rec.setPen("673213121");

        Mockito.when(batchGradAlgorithmStudentRepository.findByJobExecutionIdAndStatusIn(batchId,Arrays.asList("STARTED", "FAILED"), paging)).thenReturn(pagedData);
        Mockito.when(restUtils.getStudentData(List.of(studentId))).thenReturn(List.of(rec));
        Mockito.when(batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(rec.getStudentID(),batchId)).thenReturn(Optional.of(ent));

        ErrorDashBoard res = gradDashboardService.getErrorInfo(batchId,0,10,"accessToken");
        assertThat(res).isNotNull();
        assertThat(res.getErrorList()).hasSize(1);

    }

    @Test
    public void testGetBatchSummary() {
        Pageable paging = PageRequest.of(0, 10);
        Long batchId= 123123L;
        UUID studentId = UUID.randomUUID();
        BatchJobExecutionEntity ent = new BatchJobExecutionEntity();
        ent.setEndTime(LocalDateTime.now());
        ent.setJobExecutionId(batchId);
        ent.setStartTime(LocalDateTime.now());
        ent.setStatus("COMPLETED");
        Page<BatchJobExecutionEntity> pagedDate = new PageImpl(List.of(ent));
        Mockito.when(batchJobExecutionRepository.findAllByOrderByCreateTimeDesc(paging)).thenReturn(pagedDate);

        SummaryDashBoard res = gradDashboardService.getBatchSummary(0,10);
        assertThat(res).isNotNull();
        assertThat(res.getBatchJobList()).hasSize(1);
    }

    @Test
    public void testPurgeOldBatchHistoryRecords() {
        boolean isExceptionThrown = false;
        try {
            gradDashboardService.purgeOldBatchHistoryRecords();
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testPurgeOldSpringMetaDataRecords() {
        boolean isExceptionThrown = false;
        try {
            gradDashboardService.purgeOldSpringMetaDataRecords();
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

}
