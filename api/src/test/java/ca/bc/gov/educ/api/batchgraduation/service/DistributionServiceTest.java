package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.entity.StudentCredentialDistributionEntity;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.repository.StudentCredentialDistributionRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.util.JsonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DistributionServiceTest {

    @Autowired
    JsonUtil jsonUtil;

    @Autowired
    DistributionService distributionService;

    @MockBean
    GradBatchHistoryService gradBatchHistoryService;

    @MockBean
    StudentCredentialDistributionRepository studentCredentialDistributionRepository;

    @MockBean
    private RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Test
    public void testGetStudentCredentialDistributions() throws Exception {
        Long batchId = 3001L;
        StudentCredentialDistributionEntity entity = new StudentCredentialDistributionEntity();
        entity.setId(UUID.randomUUID());
        entity.setJobExecutionId(batchId);
        entity.setStudentID(UUID.randomUUID());
        entity.setJobType("DISTRUN");
        entity.setSchoolOfRecord("12345678");

        StudentCredentialDistribution dto = new StudentCredentialDistribution();
        dto.setId(entity.getId());
        dto.setStudentID(entity.getStudentID());
        dto.setPen("123456789");
        dto.setSchoolOfRecord(entity.getSchoolOfRecord());

        entity.setPayload(jsonUtil.getJsonStringFromObject(dto));

        when(studentCredentialDistributionRepository.findByJobExecutionId(batchId)).thenReturn(Arrays.asList(entity));

        List<StudentCredentialDistribution> response = distributionService.getStudentCredentialDistributions(batchId);
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(entity.getId());

    }

    @Test
    public void testGetSchoolListForDistribution() {
        Long batchId = 3001L;
        String schoolOfRecord = "12345678";

        when(studentCredentialDistributionRepository.getSchoolList(batchId)).thenReturn(Arrays.asList(schoolOfRecord));

        List<String> response = distributionService.getSchoolListForDistribution(batchId);
        assertThat(response).hasSize(1);
        assertThat(response.get(0)).isEqualTo(schoolOfRecord);
    }

    @Test
    public void testSaveStudentCredentialDistribution() throws Exception {
        Long batchId = 3001L;
        StudentCredentialDistributionEntity entity = new StudentCredentialDistributionEntity();
        entity.setId(UUID.randomUUID());
        entity.setJobExecutionId(batchId);
        entity.setJobType("DISTRUN");
        entity.setSchoolOfRecord("12345678");

        StudentCredentialDistribution dto = new StudentCredentialDistribution();
        dto.setId(entity.getId());
        dto.setStudentID(entity.getStudentID());
        dto.setPen("123456789");
        dto.setSchoolOfRecord(entity.getSchoolOfRecord());

        entity.setPayload(jsonUtil.getJsonStringFromObject(dto));

        when(studentCredentialDistributionRepository.save(entity)).thenReturn(entity);

        boolean isExceptionThrown = false;
        try {
            distributionService.saveStudentCredentialDistribution(entity.getJobExecutionId(), entity.getJobType(), dto);
        } catch (Exception ex) {
            isExceptionThrown = true;
        }

        assertThat(isExceptionThrown).isFalse();
    }

    @Test
    public void testGetJobTypeFromBatchJobHistory() {
        Long batchId = 3001L;

        BatchGradAlgorithmJobHistoryEntity batchGradAlgorithmJobHistoryEntity = new BatchGradAlgorithmJobHistoryEntity();
        batchGradAlgorithmJobHistoryEntity.setId(UUID.randomUUID());
        batchGradAlgorithmJobHistoryEntity.setJobExecutionId(batchId);
        batchGradAlgorithmJobHistoryEntity.setStatus("STARTED");
        batchGradAlgorithmJobHistoryEntity.setJobType("REGALG");
        batchGradAlgorithmJobHistoryEntity.setTriggerBy("MANUAL");

        when(gradBatchHistoryService.getGradAlgorithmJobHistory(batchId)).thenReturn(batchGradAlgorithmJobHistoryEntity);

        String response = distributionService.getJobTypeFromBatchJobHistory(batchId);
        assertThat(response).isEqualTo(batchGradAlgorithmJobHistoryEntity.getJobType());
    }

    @Test
    public void testUpdateDistributionBatchJobStatus() throws Exception {
        Long batchId = 3001L;
        String status = BatchStatusEnum.COMPLETED.name();
        String jobType = "DISTRUN";

        BatchGradAlgorithmJobHistoryEntity batchGradAlgorithmJobHistoryEntity = new BatchGradAlgorithmJobHistoryEntity();
        batchGradAlgorithmJobHistoryEntity.setId(UUID.randomUUID());
        batchGradAlgorithmJobHistoryEntity.setJobExecutionId(batchId);
        batchGradAlgorithmJobHistoryEntity.setStartTime(new Date(System.currentTimeMillis()));
        batchGradAlgorithmJobHistoryEntity.setExpectedStudentsProcessed(1L);
        batchGradAlgorithmJobHistoryEntity.setActualStudentsProcessed(0L);
        batchGradAlgorithmJobHistoryEntity.setFailedStudentsProcessed(0);
        batchGradAlgorithmJobHistoryEntity.setJobType(jobType);
        batchGradAlgorithmJobHistoryEntity.setStatus(status);

        when(gradBatchHistoryService.getGradAlgorithmJobHistory(batchId)).thenReturn(batchGradAlgorithmJobHistoryEntity);
        when(gradBatchHistoryService.saveGradAlgorithmJobHistory(batchGradAlgorithmJobHistoryEntity)).thenReturn(batchGradAlgorithmJobHistoryEntity);

        boolean isExceptionThrown = false;
        try {
            distributionService.updateDistributionBatchJobStatus(batchId, 0, status, null);
        } catch (Exception ex) {
            isExceptionThrown = true;
        }

        assertThat(isExceptionThrown).isFalse();
    }

}
