package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class JobLauncherControllerTest {

    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";
    private static final String SEARCH_REQUEST = "searchRequest";
    private static final String MANUAL = "MANUAL";
    private static final String TVRRUN = "TVRRUN";
    private static final String REGALG = "REGALG";
    private static final String DISTRUN = "DISTRUN";
    private static final String DISTRUNYEAREND = "DISTRUNYEAREND";
    private static final String DISTRUNUSER = "DISTRUNUSER";
    private static final String PSIDISTRUN = "PSIRUN";
    private static final String CREDENTIALTYPE = "credentialType";
    private static final String TRANMISSION_TYPE = "transmissionType";
    private static final String DISDTO = "distributionSummaryDTO";
    private static final String SCHREPORT = "SCHREP";

    @Mock
    GradDashboardService gradDashboardService;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobRegistry jobRegistry;

    @Mock
    private JobParametersBuilder jobParametersBuilder;

    @InjectMocks
    private JobLauncherController jobLauncherController;

    @MockBean
    WebClient webClient;

    @MockBean
    RestUtils restUtils;

    @Test
    public void testRegGradJob() {
        boolean exceptionIsThrown = false;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, REGALG);

        try {
            org.mockito.Mockito.when(jobLauncher.run(jobRegistry.getJob("GraduationBatchJob"), builder.toJobParameters())).thenReturn(new JobExecution(210L));
            jobLauncherController.launchRegGradJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(builder).isNotNull();
    }

    @Test
    public void testTVRJob() {
        boolean exceptionIsThrown = false;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, TVRRUN);
        try {
            org.mockito.Mockito.when(jobLauncher.run(jobRegistry.getJob("tvrBatchJob"), builder.toJobParameters())).thenReturn(new JobExecution(210L));
            jobLauncherController.launchTvrRunJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(builder).isNotNull();
    }

    @Test
    public void testLoadStudentIDs() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        LoadStudentData loadStudentData = new LoadStudentData();
        loadStudentData.setPen(pen);

        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.loadStudentIDs(Arrays.asList(loadStudentData), "");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();

    }

    @Test
    public void testDistributionGradJob() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchDistributionRunJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testSpcTVRGradJob() {
        boolean exceptionIsThrown = false;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, TVRRUN);
        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList("123213123"));
        try {
            org.mockito.Mockito.when(jobLauncher.run(jobRegistry.getJob("SpecialTvrRunBatchJob"), builder.toJobParameters())).thenReturn(new JobExecution(210L));
            jobLauncherController.launchTvrRunSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        req = new StudentSearchRequest();
        req.setPrograms(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchTvrRunSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        req = new StudentSearchRequest();
        req.setSchoolOfRecords(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchTvrRunSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        req = new StudentSearchRequest();
        req.setDistricts(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchTvrRunSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        req = new StudentSearchRequest();
        req.setSchoolCategoryCodes(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchTvrRunSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        req = new StudentSearchRequest();
        req.setPens(new ArrayList<>());
        req.setDistricts(new ArrayList<>());
        req.setSchoolCategoryCodes(new ArrayList<>());
        req.setPrograms(new ArrayList<>());
        req.setSchoolOfRecords(new ArrayList<>());

        try {
            jobLauncherController.launchTvrRunSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testlaunchUserReqDisRunSpecialJob() {
        boolean exceptionIsThrown = false;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNUSER);
        builder.addString("LocalDownload",null);
        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList("123213123"));
        try {
            org.mockito.Mockito.when(jobLauncher.run(jobRegistry.getJob("UserReqDistributionBatchJob"), builder.toJobParameters())).thenReturn(new JobExecution(210L));
            jobLauncherController.launchUserReqDisRunSpecialJob("OT",req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        req = new StudentSearchRequest();
        req.setPrograms(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchUserReqDisRunSpecialJob("OT",req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        req = new StudentSearchRequest();
        req.setSchoolOfRecords(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchUserReqDisRunSpecialJob("OT",req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        req = new StudentSearchRequest();
        req.setDistricts(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchUserReqDisRunSpecialJob("OT",req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        req = new StudentSearchRequest();
        req.setSchoolCategoryCodes(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchUserReqDisRunSpecialJob("OT",req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        req = new StudentSearchRequest();
        req.setPens(new ArrayList<>());
        req.setDistricts(new ArrayList<>());
        req.setSchoolCategoryCodes(new ArrayList<>());
        req.setPrograms(new ArrayList<>());
        req.setSchoolOfRecords(new ArrayList<>());

        try {
            jobLauncherController.launchUserReqDisRunSpecialJob("OT",req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testlaunchUserReqBlankDisRunSpecialJob_1() {
        boolean exceptionIsThrown = false;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNUSER);
        builder.addString(CREDENTIALTYPE,"OT");
        builder.addString("LocalDownload",null);

        BlankCredentialRequest req = new BlankCredentialRequest();
        req.setCredentialTypeCode(Arrays.asList("123213123"));
        req.setSchoolOfRecords(List.of("23213112"));
        try {
            org.mockito.Mockito.when(jobLauncher.run(jobRegistry.getJob("blankDistributionBatchJob"), builder.toJobParameters())).thenReturn(new JobExecution(210L));
            jobLauncherController.launchUserReqBlankDisRunSpecialJob(req,"OT");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isTrue();
    }
    @Test
    public void testlaunchUserReqBlankDisRunSpecialJob_2() {
        boolean exceptionIsThrown = false;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNUSER);
        builder.addString(CREDENTIALTYPE,"OT");
        builder.addString("LocalDownload",null);

        BlankCredentialRequest req = new BlankCredentialRequest();
        req.setSchoolOfRecords(Arrays.asList("123213123"));
        req.setCredentialTypeCode(new ArrayList<>());
        try {
            org.mockito.Mockito.when(jobLauncher.run(jobRegistry.getJob("blankDistributionBatchJob"), builder.toJobParameters())).thenReturn(new JobExecution(210L));
            jobLauncherController.launchUserReqBlankDisRunSpecialJob(req,"OT");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }
    @Test
    public void testlaunchUserReqBlankDisRunSpecialJob_3() {
        boolean exceptionIsThrown = false;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNUSER);
        builder.addString(CREDENTIALTYPE,"OT");
        builder.addString("LocalDownload",null);

        BlankCredentialRequest req = new BlankCredentialRequest();
        req.setCredentialTypeCode(new ArrayList<>());
        req.setSchoolOfRecords(new ArrayList<>());

        try {
            org.mockito.Mockito.when(jobLauncher.run(jobRegistry.getJob("blankDistributionBatchJob"), builder.toJobParameters())).thenReturn(new JobExecution(210L));
            jobLauncherController.launchUserReqBlankDisRunSpecialJob(req,"OT");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }

    @Test
    public void testlaunchYearlyDistributionRunJob() {
        boolean exceptionIsThrown = false;
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNYEAREND);
        try {
            org.mockito.Mockito.when(jobLauncher.run(jobRegistry.getJob("YearlyDistributionBatchJob"), builder.toJobParameters())).thenReturn(new JobExecution(210L));
            jobLauncherController.launchYearlyDistributionRunJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(builder).isNotNull();
    }


    @Test
    public void testlaunchUserReqPsiDisRunSpecialJob_1() {
        boolean exceptionIsThrown = false;
        PsiCredentialRequest req = new PsiCredentialRequest();
        req.setPsiYear("2018");
        req.setPsiCodes(List.of("001"));
        try {
            jobLauncherController.launchUserReqPsiDisRunSpecialJob(req,"PAPER");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isTrue();
    }
    @Test
    public void testlaunchUserReqPsiDisRunSpecialJob_2() {
        boolean exceptionIsThrown = false;
        PsiCredentialRequest req = new PsiCredentialRequest();
        req.setPsiCodes(Arrays.asList("123213123"));
        req.setPsiYear("");
        try {
            jobLauncherController.launchUserReqPsiDisRunSpecialJob(req,"PAPER");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }
    @Test
    public void testlaunchUserReqPsiDisRunSpecialJob_3() {
        boolean exceptionIsThrown = false;
        PsiCredentialRequest req = new PsiCredentialRequest();
        req.setPsiYear("");
        req.setPsiCodes(new ArrayList<>());

        try {
            jobLauncherController.launchUserReqPsiDisRunSpecialJob(req,"PAPER");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }
        assertThat(exceptionIsThrown).isFalse();
    }



    @Test
    public void testSpcRegGradJob() {
        boolean exceptionIsThrown = false;
        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchRegGradSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testLoadDashboard() {
        GradDashboard dash = new GradDashboard();
        dash.setBatchInfoList(new ArrayList<>());
        dash.setLastActualStudentsProcessed("10");
        dash.setLastExpectedStudentsProcessed("20");
        dash.setLastFailedStudentsProcessed("10");

        org.mockito.Mockito.when(gradDashboardService.getDashboardInfo()).thenReturn(dash);
        jobLauncherController.loadDashboard();
        org.mockito.Mockito.verify(gradDashboardService).getDashboardInfo();
    }

    @Test
    public void testLoadDashboard_null() {
        org.mockito.Mockito.when(gradDashboardService.getDashboardInfo()).thenReturn(null);
        jobLauncherController.loadDashboard();
        org.mockito.Mockito.verify(gradDashboardService).getDashboardInfo();
    }

    @Test
    public void testloadSummary() {
        SummaryDashBoard dash = new SummaryDashBoard();
        dash.setBatchJobList(new ArrayList<>());
        dash.setNumber(0);
        dash.setSize(10);

        org.mockito.Mockito.when(gradDashboardService.getBatchSummary(0,10)).thenReturn(dash);
        jobLauncherController.loadSummary(0,10);
        org.mockito.Mockito.verify(gradDashboardService).getBatchSummary(0,10);
    }
    @Test
    public void testloadSummary_null() {
        org.mockito.Mockito.when(gradDashboardService.getBatchSummary(0,10)).thenReturn(null);
        jobLauncherController.loadSummary(0,10);
        org.mockito.Mockito.verify(gradDashboardService).getBatchSummary(0,10);
    }


    @Test
    public void testLoadError() {
        ErrorDashBoard dash = new ErrorDashBoard();
        dash.setErrorList(new ArrayList<>());
        dash.setNumber(0);
        dash.setSize(10);

        org.mockito.Mockito.when(gradDashboardService.getErrorInfo(1234L,0,10,"accessToken")).thenReturn(dash);
        jobLauncherController.loadError(1234L,0,10,"accessToken");
        org.mockito.Mockito.verify(gradDashboardService).getErrorInfo(1234L,0,10,"accessToken");
    }

    @Test
    public void testLoadError_null() {
        org.mockito.Mockito.when(gradDashboardService.getDashboardInfo()).thenReturn(null);
        jobLauncherController.loadDashboard();
        org.mockito.Mockito.verify(gradDashboardService).getDashboardInfo();
    }
}
