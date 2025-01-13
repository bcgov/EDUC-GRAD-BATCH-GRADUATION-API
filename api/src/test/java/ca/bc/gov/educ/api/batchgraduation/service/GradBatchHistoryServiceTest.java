package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmStudentRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GradBatchHistoryServiceTest {

    @Autowired
    GradBatchHistoryService gradBatchHistoryService;

    @MockBean
    BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;

    @MockBean
    BatchGradAlgorithmStudentRepository batchGradAlgorithmStudentRepository;

    @Test
    public void testGetGradAlgorithmJobHistory() {
        Long batchId = 3001L;

        BatchGradAlgorithmJobHistoryEntity batchGradAlgorithmJobHistoryEntity = new BatchGradAlgorithmJobHistoryEntity();
        batchGradAlgorithmJobHistoryEntity.setId(UUID.randomUUID());
        batchGradAlgorithmJobHistoryEntity.setJobExecutionId(batchId);
        batchGradAlgorithmJobHistoryEntity.setStatus("STARTED");
        batchGradAlgorithmJobHistoryEntity.setJobType("REGALG");
        batchGradAlgorithmJobHistoryEntity.setTriggerBy("MANUAL");

        when(batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(batchId)).thenReturn(Optional.of(batchGradAlgorithmJobHistoryEntity));

        BatchGradAlgorithmJobHistoryEntity response = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        assertThat(response).isNotNull();
        assertThat(response.getJobExecutionId()).isEqualTo(batchId);
    }

    @Test
    public void testGetGradAlgorithmJobHistory_when_givenBatchId_does_notExist() {
        Long batchId = 3001L;

        when(batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(batchId)).thenReturn(Optional.empty());

        BatchGradAlgorithmJobHistoryEntity response = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        assertThat(response).isNull();
    }

    @Test
    public void testSaveGradAlgorithmJobHistory_when_creating_JobHistory() {
        Long batchId = 3001L;

        BatchGradAlgorithmJobHistoryEntity batchGradAlgorithmJobHistoryEntity = new BatchGradAlgorithmJobHistoryEntity();
        batchGradAlgorithmJobHistoryEntity.setId(UUID.randomUUID());
        batchGradAlgorithmJobHistoryEntity.setJobExecutionId(batchId);
        batchGradAlgorithmJobHistoryEntity.setStatus("STARTED");
        batchGradAlgorithmJobHistoryEntity.setJobType("REGALG");
        batchGradAlgorithmJobHistoryEntity.setTriggerBy("MANUAL");

        when(batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(batchId)).thenReturn(Optional.empty());
        when(batchGradAlgorithmJobHistoryRepository.save(batchGradAlgorithmJobHistoryEntity)).thenReturn(batchGradAlgorithmJobHistoryEntity);
        BatchGradAlgorithmJobHistoryEntity response = gradBatchHistoryService.saveGradAlgorithmJobHistory(batchGradAlgorithmJobHistoryEntity);
        assertThat(response).isNotNull();

    }

    @Test
    public void testSaveGradAlgorithmJobHistory_when_updating_JobHistory() {
        Long batchId = 3001L;

        BatchGradAlgorithmJobHistoryEntity batchGradAlgorithmJobHistoryEntity = new BatchGradAlgorithmJobHistoryEntity();
        batchGradAlgorithmJobHistoryEntity.setId(UUID.randomUUID());
        batchGradAlgorithmJobHistoryEntity.setJobExecutionId(batchId);
        batchGradAlgorithmJobHistoryEntity.setStatus("STARTED");
        batchGradAlgorithmJobHistoryEntity.setJobType("REGALG");
        batchGradAlgorithmJobHistoryEntity.setTriggerBy("MANUAL");

        when(batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(batchId)).thenReturn(Optional.of(batchGradAlgorithmJobHistoryEntity));
        when(batchGradAlgorithmJobHistoryRepository.save(batchGradAlgorithmJobHistoryEntity)).thenReturn(batchGradAlgorithmJobHistoryEntity);
        BatchGradAlgorithmJobHistoryEntity response = gradBatchHistoryService.saveGradAlgorithmJobHistory(batchGradAlgorithmJobHistoryEntity);
        assertThat(response).isNotNull();
    }

    @Test
    public void testSaveGradAlgorithmStudents() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity.setStudentID(studentId);
        batchGradAlgorithmStudentEntity.setStatus("STARTED");

        when(batchGradAlgorithmStudentRepository.saveAll(Arrays.asList(batchGradAlgorithmStudentEntity))).thenReturn(Arrays.asList(batchGradAlgorithmStudentEntity));
        boolean isExceptionThrown = false;
        try {
            gradBatchHistoryService.saveGradAlgorithmStudents(Arrays.asList(batchGradAlgorithmStudentEntity));
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testSaveGradAlgorithmStudent() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity.setStudentID(studentId);
        batchGradAlgorithmStudentEntity.setStatus("STARTED");

        when(batchGradAlgorithmStudentRepository.save(batchGradAlgorithmStudentEntity)).thenReturn(batchGradAlgorithmStudentEntity);
        BatchGradAlgorithmStudentEntity response = gradBatchHistoryService.saveGradAlgorithmStudent(batchGradAlgorithmStudentEntity);
        assertThat(response).isNotNull();
        assertThat(response.getStudentID()).isEqualTo(studentId);
        assertThat(response.getJobExecutionId()).isEqualTo(batchId);
    }

    @Test
    public void testGetErroredStudents() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity.setStudentID(studentId);
        batchGradAlgorithmStudentEntity.setStatus("FAILED");

        when(batchGradAlgorithmStudentRepository.findByJobExecutionIdAndStatusIn(batchId, Arrays.asList(BatchStatusEnum.STARTED.name(), BatchStatusEnum.FAILED.name()))).thenReturn(Arrays.asList(batchGradAlgorithmStudentEntity));
        List<BatchGradAlgorithmStudentEntity> response = gradBatchHistoryService.getErroredStudents(batchId);
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getJobExecutionId()).isEqualTo(batchId);
        assertThat(response.get(0).getStudentID()).isEqualTo(studentId);
    }

    @Test
    public void testGetAllStudents() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity.setStudentID(studentId);
        batchGradAlgorithmStudentEntity.setStatus("FAILED");

        UUID studentId2 = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity2 = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity2.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity2.setStudentID(studentId2);
        batchGradAlgorithmStudentEntity2.setStatus("STARTED");

        when(batchGradAlgorithmStudentRepository.findByJobExecutionId(batchId)).thenReturn(Arrays.asList(batchGradAlgorithmStudentEntity, batchGradAlgorithmStudentEntity2));
        List<BatchGradAlgorithmStudentEntity> response = gradBatchHistoryService.getAllStudents(batchId);
        assertThat(response).hasSize(2);
        assertThat(response.get(0).getJobExecutionId()).isEqualTo(batchId);
        assertThat(response.get(1).getJobExecutionId()).isEqualTo(batchId);
    }

    @Test
    public void testGetBatchGradAlgorithmStudent() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity.setStudentID(studentId);
        batchGradAlgorithmStudentEntity.setStatus("STARTED");

        when(batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentId, batchId)).thenReturn(Optional.of(batchGradAlgorithmStudentEntity));
        BatchGradAlgorithmStudentEntity response = gradBatchHistoryService.getBatchGradAlgorithmStudent(batchId, studentId);
        assertThat(response).isNotNull();
        assertThat(response.getStudentID()).isEqualTo(studentId);
        assertThat(response.getJobExecutionId()).isEqualTo(batchId);
    }

    @Test
    public void testGetBatchGradAlgorithmStudent_when_batchAlgorithmStudent_does_notExist() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();

        when(batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentId, batchId)).thenReturn(Optional.empty());
        BatchGradAlgorithmStudentEntity response = gradBatchHistoryService.getBatchGradAlgorithmStudent(batchId, studentId);
        assertThat(response).isNull();
    }

    @Test
    public void testSaveBatchAlgorithmStudent_when_creatingNew() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = getBatchGradAlgorithmStudentEntity(batchId, studentId, schoolId);

        when(batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentId, batchId)).thenReturn(Optional.empty());
        when(batchGradAlgorithmStudentRepository.save(batchGradAlgorithmStudentEntity)).thenReturn(batchGradAlgorithmStudentEntity);
        boolean isExceptionThrown = false;
        try {
            gradBatchHistoryService.saveBatchAlgorithmStudent(batchId, studentId, batchGradAlgorithmStudentEntity.getProgram(), schoolId);
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testSaveBatchAlgorithmStudent_when_updatingTheExistingOne() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = getBatchGradAlgorithmStudentEntity(batchId, studentId, schoolId);

        when(batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentId, batchId)).thenReturn(Optional.of(batchGradAlgorithmStudentEntity));
        when(batchGradAlgorithmStudentRepository.save(batchGradAlgorithmStudentEntity)).thenReturn(batchGradAlgorithmStudentEntity);
        boolean isExceptionThrown = false;
        try {
            gradBatchHistoryService.saveBatchAlgorithmStudent(batchId, studentId, batchGradAlgorithmStudentEntity.getProgram(), schoolId);
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    private static BatchGradAlgorithmStudentEntity getBatchGradAlgorithmStudentEntity(Long batchId, UUID studentId, UUID schoolId) {
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity.setStudentID(studentId);
        batchGradAlgorithmStudentEntity.setStatus("STARTED");
        batchGradAlgorithmStudentEntity.setProgram("Test Program");
        batchGradAlgorithmStudentEntity.setSchoolOfRecordId(schoolId);
        return batchGradAlgorithmStudentEntity;
    }

    @Test
    public void testUpdateBatchStatusForStudent() {
        Long batchId = 3001L;
        UUID studentId = UUID.randomUUID();
        UUID schoolId = UUID.randomUUID();
        BatchGradAlgorithmStudentEntity batchGradAlgorithmStudentEntity = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity.setStudentID(studentId);
        batchGradAlgorithmStudentEntity.setStatus(BatchStatusEnum.STARTED.name());
        batchGradAlgorithmStudentEntity.setProgram("Test Program");
        batchGradAlgorithmStudentEntity.setSchoolOfRecordId(schoolId);

        BatchGradAlgorithmStudentEntity savedEntity = new BatchGradAlgorithmStudentEntity();
        batchGradAlgorithmStudentEntity.setJobExecutionId(batchId);
        batchGradAlgorithmStudentEntity.setStudentID(studentId);
        batchGradAlgorithmStudentEntity.setStatus(BatchStatusEnum.FAILED.name());
        batchGradAlgorithmStudentEntity.setProgram("Test Program");
        batchGradAlgorithmStudentEntity.setSchoolOfRecordId(schoolId);
        batchGradAlgorithmStudentEntity.setError("Unexpected Error");

        when(batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentId, batchId)).thenReturn(Optional.of(batchGradAlgorithmStudentEntity));
        when(batchGradAlgorithmStudentRepository.save(batchGradAlgorithmStudentEntity)).thenReturn(savedEntity);
        boolean isExceptionThrown = false;
        try {
            gradBatchHistoryService.updateBatchStatusForStudent(batchId, studentId, BatchStatusEnum.FAILED, "Unexpected Error");
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testGetCountForErroredStudent() {
        Long batchId = 3001L;
        when(batchGradAlgorithmStudentRepository.countAllByJobExecutionIdAndStatusIn(batchId, Arrays.asList(BatchStatusEnum.FAILED.name(), BatchStatusEnum.STARTED.name()))).thenReturn(2L);
        long cnt = gradBatchHistoryService.getCountForErroredStudent(batchId);
        assertThat(cnt).isEqualTo(2);
    }

    @Test
    public void testGetCountForReadStudent() {
        Long batchId = 3001L;
        when(batchGradAlgorithmStudentRepository.countAllByJobExecutionId(batchId)).thenReturn(3L);
        long cnt = gradBatchHistoryService.getCountForReadStudent(batchId);
        assertThat(cnt).isEqualTo(3);
    }

    @Test
    public void testGetCountForProcessedStudent() {
        Long batchId = 3001L;
        when(batchGradAlgorithmStudentRepository.countAllByJobExecutionIdAndStatus(batchId, BatchStatusEnum.COMPLETED.name())).thenReturn(2L);
        long cnt = gradBatchHistoryService.getCountForProcessedStudent(batchId);
        assertThat(cnt).isEqualTo(2);
    }

    @Test
    public void testGetSchoolListForReport() {
        Long batchId = 3001L;
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        when(batchGradAlgorithmStudentRepository.getSchoolList(batchId)).thenReturn(Arrays.asList(schoolId1, schoolId2));
        List<UUID> list = gradBatchHistoryService.getSchoolListForReport(batchId);
        assertThat(list).hasSize(2);
    }

    @Test
    public void testGetCopyAllStudentsIntoNewBatch() {
        Long batchId = 3001L;
        Long oldBatchId = 3000L;
        String username = "TestUser";
        Mockito.doNothing().when(batchGradAlgorithmStudentRepository).copyAllGradAlgorithmStudents(eq(batchId), eq(oldBatchId), eq(username), any(Date.class));

        boolean isExceptionThrown = false;
        try {
            gradBatchHistoryService.copyAllStudentsIntoNewBatch(batchId, oldBatchId, username);
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testGetCopyErroredStudentsIntoNewBatch() {
        Long batchId = 3001L;
        Long oldBatchId = 3000L;
        String username = "TestUser";
        Mockito.doNothing().when(batchGradAlgorithmStudentRepository).copyGradAlgorithmErroredStudents(eq(batchId), eq(oldBatchId), eq(username), any(Date.class));

        boolean isExceptionThrown = false;
        try {
            gradBatchHistoryService.copyErroredStudentsIntoNewBatch(batchId, oldBatchId, username);
        } catch (Exception e) {
            isExceptionThrown = true;
        }
        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testGetGraduationProgramCountsForBatchRunSummary() {
        Long batchId = 3001L;

        List<Object[]> gradCounts = new ArrayList<>();
        Object[] count1 = new Object[] {"2018-EN", BigDecimal.valueOf(10)};
        Object[] count2 = new Object[] {"2018-PF", BigDecimal.valueOf(2)};
        Object[] count3 = new Object[] {"2004-EN", BigDecimal.valueOf(8)};
        Object[] count4 = new Object[] {"1950", BigDecimal.valueOf(15)};
        gradCounts.addAll(Arrays.asList(count1, count2, count3, count4));

        when(batchGradAlgorithmStudentRepository.getGraduationProgramCounts(batchId)).thenReturn(gradCounts);
        Map<String, Integer> response = gradBatchHistoryService.getGraduationProgramCountsForBatchRunSummary(batchId);
        assertThat(response).hasSize(4).containsEntry("2018-EN", Integer.valueOf(10));
    }

}
