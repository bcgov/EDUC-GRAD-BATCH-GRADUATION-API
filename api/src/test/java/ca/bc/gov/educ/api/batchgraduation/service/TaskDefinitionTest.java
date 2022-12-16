package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.UserScheduledJobsEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.UserScheduledJobsRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import net.javacrumbs.shedlock.spring.LockableTaskScheduler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class TaskDefinitionTest {

    @Autowired
    TaskDefinition taskDefinition;

    @MockBean
    @Qualifier("lockableTaskScheduler")
    LockableTaskScheduler taskScheduler;

    @Autowired
    Job SpecialGraduationBatchJob;

    @MockBean
    JobLauncher jobLauncher;

    @MockBean
    private RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @MockBean
    JobRegistry jobRegistry;

    @MockBean
    GradBatchHistoryService gradBatchHistoryService;

    @MockBean
    UserScheduledJobsRepository userScheduledJobsRepository;

    @Test
    public void testRun() {
        Task task = new Task();

        task.setJobName("SGBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        taskDefinition.setTask(task);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withblankpayload() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("SGBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        task.setLocalDownload("Y");
        task.setDeliveredToUser(true);
        task.setCredentialType("OT");
        task.setProperUserName("GREG");
        BlankCredentialRequest bReq = new BlankCredentialRequest();
        bReq.setQuantity(5);
        bReq.setSchoolOfRecords(List.of("123112311"));
        bReq.setCredentialTypeCode(List.of("E"));
        task.setBlankPayLoad(bReq);
        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withregularpayload() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("SGBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        StudentSearchRequest bReq = new StudentSearchRequest();
        bReq.setPens(List.of("12312311145"));
        bReq.setSchoolCategoryCodes(new ArrayList<>());
        task.setPayload(bReq);
        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withemptyblankpayload() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("SGBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        task.setJobIdReference(null);
        task.setJobParams("342342");
        BlankCredentialRequest bReq = new BlankCredentialRequest();
        bReq.setQuantity(5);
        bReq.setSchoolOfRecords(new ArrayList<>());
        bReq.setCredentialTypeCode(new ArrayList<>());
        task.setBlankPayLoad(bReq);
        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withemptyblankpayload_2() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("SGBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        task.setJobIdReference(UUID.randomUUID());
        task.setJobParams(null);
        BlankCredentialRequest bReq = new BlankCredentialRequest();
        bReq.setQuantity(5);
        bReq.setSchoolOfRecords(new ArrayList<>());
        bReq.setCredentialTypeCode(List.of("E"));
        task.setBlankPayLoad(bReq);
        taskDefinition.setTask(task);

        UserScheduledJobsEntity userScheduledJobsEntity = new UserScheduledJobsEntity();
        userScheduledJobsEntity.setId(task.getJobIdReference());
        userScheduledJobsEntity.setStatus("QUEUED");
        userScheduledJobsEntity.setJobName(task.getJobName());
        userScheduledJobsEntity.setJobCode(task.getJobName());
        userScheduledJobsEntity.setCronExpression(task.getCronExpression());

        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        Mockito.when(userScheduledJobsRepository.findById(task.getJobIdReference())).thenReturn(Optional.of(userScheduledJobsEntity));
        taskDefinition.run();

        bReq = new BlankCredentialRequest();
        bReq.setQuantity(5);
        bReq.setSchoolOfRecords(new ArrayList<>());
        bReq.setCredentialTypeCode(List.of("E"));
        task.setBlankPayLoad(bReq);
        taskDefinition.setTask(task);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withemptyblankpayload_3() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("SGBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        task.setJobIdReference(UUID.randomUUID());
        task.setJobParams("342342");
        BlankCredentialRequest bReq = new BlankCredentialRequest();
        bReq.setQuantity(5);
        bReq.setSchoolOfRecords(List.of("E"));
        bReq.setCredentialTypeCode(new ArrayList<>());
        task.setBlankPayLoad(bReq);
        taskDefinition.setTask(task);

        UserScheduledJobsEntity userScheduledJobsEntity = new UserScheduledJobsEntity();
        userScheduledJobsEntity.setId(task.getJobIdReference());
        userScheduledJobsEntity.setStatus("QUEUED");
        userScheduledJobsEntity.setJobName(task.getJobName());
        userScheduledJobsEntity.setJobCode(task.getJobName());
        userScheduledJobsEntity.setCronExpression(task.getCronExpression());

        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        Mockito.when(userScheduledJobsRepository.findById(task.getJobIdReference())).thenReturn(Optional.of(userScheduledJobsEntity));
        taskDefinition.run();

        bReq = new BlankCredentialRequest();
        bReq.setQuantity(5);
        bReq.setSchoolOfRecords(new ArrayList<>());
        bReq.setCredentialTypeCode(List.of("E"));
        task.setBlankPayLoad(bReq);
        taskDefinition.setTask(task);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withemptyregularpayload() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("SGBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        StudentSearchRequest bReq = new StudentSearchRequest();
        bReq.setPens(new ArrayList<>());
        bReq.setDistricts(new ArrayList<>());
        bReq.setPrograms(new ArrayList<>());
        bReq.setSchoolCategoryCodes(new ArrayList<>());
        bReq.setSchoolOfRecords(new ArrayList<>());
        task.setPayload(bReq);
        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withemptyregularpayload2() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("SGBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        StudentSearchRequest bReq = new StudentSearchRequest();
        bReq.setPens(new ArrayList<>());
        bReq.setDistricts(List.of("003"));
        bReq.setPrograms(new ArrayList<>());
        bReq.setSchoolCategoryCodes(new ArrayList<>());
        bReq.setSchoolOfRecords(new ArrayList<>());
        task.setPayload(bReq);
        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();

        bReq = new StudentSearchRequest();
        bReq.setPens(new ArrayList<>());
        bReq.setDistricts(new ArrayList<>());
        bReq.setPrograms(List.of("003"));
        bReq.setSchoolCategoryCodes(new ArrayList<>());
        bReq.setSchoolOfRecords(new ArrayList<>());
        task.setPayload(bReq);
        taskDefinition.setTask(task);
        taskDefinition.run();

        bReq = new StudentSearchRequest();
        bReq.setPens(new ArrayList<>());
        bReq.setDistricts(new ArrayList<>());
        bReq.setPrograms(new ArrayList<>());
        bReq.setSchoolCategoryCodes(List.of("003"));
        bReq.setSchoolOfRecords(new ArrayList<>());
        task.setPayload(bReq);
        taskDefinition.setTask(task);
        taskDefinition.run();

        bReq = new StudentSearchRequest();
        bReq.setPens(new ArrayList<>());
        bReq.setDistricts(new ArrayList<>());
        bReq.setPrograms(new ArrayList<>());
        bReq.setSchoolCategoryCodes(new ArrayList<>());
        bReq.setSchoolOfRecords(List.of("003"));
        task.setPayload(bReq);
        taskDefinition.setTask(task);
        taskDefinition.run();

        assertNotNull(task);
    }

    @Test
    public void testRun_withpsipayload() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("URPDBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);
        task.setTransmissionType("PAPER");
        task.setLocalDownload("N");

        PsiCredentialRequest bReq = new PsiCredentialRequest();
        bReq.setPsiCodes(List.of("12312311145"));
        bReq.setPsiYear("2021");
        task.setPsiPayLoad(bReq);

        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withpsipayload_validatenotnull() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("URPDBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);

        PsiCredentialRequest bReq = new PsiCredentialRequest();
        bReq.setPsiCodes(List.of("12312311145"));
        bReq.setPsiYear("");
        task.setPsiPayLoad(bReq);

        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withpsipayload_validatenotnull_2() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("URPDBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);

        PsiCredentialRequest bReq = new PsiCredentialRequest();
        bReq.setPsiCodes(new ArrayList<>());
        bReq.setPsiYear("2021");
        task.setPsiPayLoad(bReq);

        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();
        assertNotNull(task);
    }

    @Test
    public void testRun_withpsipayload_validatenotnull_3() throws NoSuchJobException {
        Task task = new Task();

        task.setJobName("URPDBJ");
        task.setCronExpression("0 0 20 * * *");
        task.setProperUserName(null);
        task.setDeliveredToUser(false);
        task.setCredentialType(null);

        PsiCredentialRequest bReq = new PsiCredentialRequest();
        bReq.setPsiCodes(new ArrayList<>());
        bReq.setPsiYear("");
        task.setPsiPayLoad(bReq);

        taskDefinition.setTask(task);
        Mockito.when(jobRegistry.getJob("SpecialGraduationBatchJob")).thenReturn(SpecialGraduationBatchJob);
        taskDefinition.run();
        assertNotNull(task);
    }

}
