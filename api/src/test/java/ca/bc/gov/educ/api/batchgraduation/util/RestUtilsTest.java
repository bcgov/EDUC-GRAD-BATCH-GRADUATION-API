package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RESTService;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
import lombok.val;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RestUtilsTest {

    @Autowired
    GraduationReportService graduationReportService;

    @Autowired
    RestUtils restUtils;

    @MockBean
    TokenUtils tokenUtils;

    @MockBean
    RESTService restService;

    @MockBean
    @Qualifier("webClient")
    WebClient webClient;

    @MockBean
    @Qualifier("batchClient")
    WebClient batchWebClient;

    @Autowired
    JsonTransformer jsonTransformer;

    @Autowired
    private EducGradBatchGraduationApiConstants constants;
    @Mock
    Logger LOGGER = LoggerFactory.getLogger(RestUtils.class);

    @Before
    public void setUp() {
        openMocks(this);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testGetTokenResponseObject_returnsToken_with_APICallSuccess() {
        String mockToken = mockTokenResponseObject();

        val result = this.restUtils.getTokenResponseObject();
        assertThat(result).isNotNull();
        assertThat(result.getAccess_token()).isEqualTo(mockToken);
        assertThat(result.getRefresh_token()).isEqualTo("456");
    }

    @Test
    public void testGetStudentByPen_givenValues_returnsStudent_with_APICallSuccess() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        final String pen = "123456789";
        student.setStudentID(studentID);
        student.setPen(pen);

        when(this.restService.get(String.format(constants.getPenStudentApiByPenUrl(), pen), List.class)).thenReturn(Arrays.asList(student));

        val result = this.restUtils.getStudentsByPen(pen);
        assertThat(result).isNotNull();
        assertThat(result.size()).isPositive();
        assertThat(result.get(0).getPen()).isEqualTo(pen);

        val result2 = this.restUtils.getStudentIDByPen(pen);
        assertThat(result2).isNotNull();

    }

    @Test
    public void testSaveGraduationStatus_givenValues_returnsGraduationStatus_with_APICallSuccess() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        when(this.restService.post(String.format(constants.getGradStudentApiGradStatusUrl(), studentID), graduationStatus, GraduationStudentRecord.class)).thenReturn(graduationStatus);

        var result = this.restUtils.saveGraduationStudentRecord(graduationStatus);
        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(pen);
    }

    @Test
    public void testGetStudentsForSpecialGradRun_with_APICallSuccess() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList(pen));

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        GraduationStudentRecordSearchResult res = new GraduationStudentRecordSearchResult();
        res.setStudentIDs(Arrays.asList(graduationStatus.getStudentID()));

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradStudentApiStudentForSpcGradListUrl(), studentID), req, GraduationStudentRecordSearchResult.class)).thenReturn(res);

        var result = this.restUtils.getStudentsForSpecialGradRun(req);
        assertThat(result).isNotNull();
        assertThat(result.get(0)).isEqualTo(studentID);
    }

    @Test
    public void testGetStudentsForSpecialGradRun_with_APICallSuccess_null() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList(pen));

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradStudentApiStudentForSpcGradListUrl(), studentID), req, GraduationStudentRecordSearchResult.class)).thenReturn(null);

        var result = this.restUtils.getStudentsForSpecialGradRun(req);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testProcessStudent() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(graduationStatus);
        alres.setStudentOptionalProgram(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setBatchId(batchId);

        when(this.restService.get(String.format(constants.getGraduationApiUrl(), studentID,batchId), AlgorithmResponse.class)).thenReturn(alres);

        GraduationStudentRecord response = this.restUtils.processStudent(graduationStatus,summary);
        assertThat(response.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testProcessStudent_witherrors() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(graduationStatus);
        alres.setStudentOptionalProgram(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();

        when(this.restService.get(String.format(constants.getGraduationApiUrl(), studentID,batchId), AlgorithmResponse.class)).thenReturn(alres);

        GraduationStudentRecord response = this.restUtils.processStudent(graduationStatus,summary);
        assertNull(response);
    }

    @Test
    public void testProcessStudent_witherrors2() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(null);
        alres.setStudentOptionalProgram(null);
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setExceptionName("REPAPI DOWN");
        exceptionMessage.setExceptionDetails("DOWN DOWN DOWN");
        alres.setException(exceptionMessage);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setBatchId(batchId);

        when(this.restService.get(String.format(constants.getGraduationApiUrl(), studentID,batchId), AlgorithmResponse.class)).thenReturn(alres);

        GraduationStudentRecord response = this.restUtils.processStudent(graduationStatus,summary);
        assertNull(response);
    }

    @Test
    public void testProcessProjectedStudent() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(graduationStatus);
        alres.setStudentOptionalProgram(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setBatchId(batchId);

        when(this.restService.get(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId), AlgorithmResponse.class)).thenReturn(alres);

        GraduationStudentRecord response = this.restUtils.processProjectedGradStudent(graduationStatus,summary);
        assertThat(response.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testProcessProjectedStudent_witherrors() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(graduationStatus);
        alres.setStudentOptionalProgram(null);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();

        when(this.restService.get(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId), AlgorithmResponse.class)).thenReturn(alres);

        GraduationStudentRecord response = this.restUtils.processProjectedGradStudent(graduationStatus,summary);
        assertNull(response);
    }

    @Test
    public void testProcessProjectedStudent_witherrors2() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        AlgorithmResponse alres = new AlgorithmResponse();
        alres.setGraduationStudentRecord(null);
        alres.setStudentOptionalProgram(null);
        ExceptionMessage exceptionMessage = new ExceptionMessage();
        exceptionMessage.setExceptionName("REPAPI DOWN");
        exceptionMessage.setExceptionDetails("DOWN DOWN DOWN");
        alres.setException(exceptionMessage);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setBatchId(batchId);

        mockTokenResponseObject();

        when(this.restService.get(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId), AlgorithmResponse.class)).thenReturn(alres);

        GraduationStudentRecord response = this.restUtils.processProjectedGradStudent(graduationStatus,summary);
        assertNull(response);
    }

    @Test
    public void testGetStudentData_withlist() {

        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";

        List<UUID> studentList = Arrays.asList(studentID);

        StudentList stuList = new StudentList();
        stuList.setStudentids(studentList);

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradStudentApiStudentDataListUrl()), stuList, List.class)).thenReturn(List.of(graduationStatus));

        List<GraduationStudentRecord> resList =  this.restUtils.getStudentData(studentList);
        assertNotNull(resList);
        assertThat(resList).hasSize(1);
    }

    @Test
    public void testProcessDistribution() {

        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final Long batchId = 9879L;
        final String mincode = "123121111";
        List<StudentCredentialDistribution> globalList = new ArrayList<>();

        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setStudentGrade("12");
        scd.setId(UUID.randomUUID());
        scd.setPaperType("YED4");
        scd.setSchoolOfRecord(mincode);
        scd.setStudentID(studentID);
        globalList.add(scd);

        DistributionSummaryDTO summary = new DistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd,summary, false);
        assertNotNull(res);
    }

    @Test
    public void testProcessDistribution_elsecase() {

        final UUID studentID = UUID.randomUUID();
        final UUID studentID2 = UUID.randomUUID();
        final Long batchId = 9879L;
        final UUID schoolId = UUID.randomUUID();
        List<StudentCredentialDistribution> globalList = new ArrayList<>();

        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setStudentGrade("12");
        scd.setId(UUID.randomUUID());
        scd.setPaperType("YED4");
        scd.setSchoolId(schoolId);
        scd.setStudentID(studentID);
        globalList.add(scd);

        StudentCredentialDistribution scd2 = new StudentCredentialDistribution();
        scd2.setStudentGrade("12");
        scd2.setId(UUID.randomUUID());
        scd2.setPaperType("YED4");
        scd2.setSchoolId(schoolId);
        scd2.setStudentID(studentID2);


        GraduationStudentRecordDistribution grd = new GraduationStudentRecordDistribution();
        grd.setStudentID(studentID2);
        grd.setProgram("2018-EN");
        grd.setStudentGrade("12");
        grd.setSchoolOfRecordId(schoolId);

        mockTokenResponseObject();

        when(this.restService.get(String.format(constants.getStudentInfo(),studentID2), GraduationStudentRecordDistribution.class)).thenReturn(grd);

        DistributionSummaryDTO summary = new DistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd2,summary, false);
        assertNotNull(res);
    }

    @Test
    public void testProcessDistribution_elsecase_null() {

        final UUID studentID = UUID.randomUUID();
        final UUID studentID2 = UUID.randomUUID();
        final Long batchId = 9879L;
        final UUID schoolId = UUID.randomUUID();
        List<StudentCredentialDistribution> globalList = new ArrayList<>();

        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setStudentGrade("12");
        scd.setId(UUID.randomUUID());
        scd.setPaperType("YED4");
        scd.setSchoolId(schoolId);
        scd.setStudentID(studentID);
        globalList.add(scd);

        StudentCredentialDistribution scd2 = new StudentCredentialDistribution();
        scd2.setStudentGrade("12");
        scd2.setId(UUID.randomUUID());
        scd2.setPaperType("YED4");
        scd2.setSchoolId(schoolId);
        scd2.setStudentID(studentID2);

        mockTokenResponseObject();

        GraduationStudentRecordDistribution grd = new GraduationStudentRecordDistribution();
        grd.setStudentID(studentID2);
        grd.setProgram("2018-EN");
        grd.setStudentGrade("12");
        grd.setSchoolOfRecordId(schoolId);

        when(this.restService.get(String.format(constants.getStudentInfo(),studentID2), GraduationStudentRecordDistribution.class)).thenReturn(null);


        DistributionSummaryDTO summary = new DistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        StudentCredentialDistribution res = this.restUtils.processDistribution(scd2,summary, false);
        assertNotNull(res);
    }

    @Test
    public void testProcessBlankDistribution() {
        final String credentialType = "OT";
        BlankDistributionSummaryDTO summary = new BlankDistributionSummaryDTO();
        summary.setCredentialType("OT");
        summary.setBatchId(4564L);

        BlankCredentialDistribution bcd = new BlankCredentialDistribution();
        bcd.setQuantity(5);
        bcd.setCredentialTypeCode("BC1996-PUB");

        BlankCredentialDistribution res = this.restUtils.processBlankDistribution(bcd,summary);
        assertNotNull(res);
    }
    @Test
    public void testProcessBlankDistribution_null() {
        final String credentialType = null;
        BlankDistributionSummaryDTO summary = new BlankDistributionSummaryDTO();
        summary.setCredentialType(null);
        summary.setBatchId(4564L);

        BlankCredentialDistribution bcd = new BlankCredentialDistribution();
        bcd.setQuantity(5);
        bcd.setCredentialTypeCode("BC1996-PUB");

        BlankCredentialDistribution res = this.restUtils.processBlankDistribution(bcd,summary);
        assertNotNull(res);
    }


    @Test
    public void testProcessBlankDistribution_OC() {
        final String credentialType = "OC";
        BlankDistributionSummaryDTO summary = new BlankDistributionSummaryDTO();
        summary.setCredentialType("OC");
        summary.setBatchId(4564L);

        GradCertificateTypes certificateTypes = new GradCertificateTypes();
        certificateTypes.setPaperType("YED2");
        certificateTypes.setCode("E");
        certificateTypes.setDescription("SDS");
        certificateTypes.setLabel("fere");

        when(this.restService.get(String.format(constants.getCertificateTypes(),"E"), GradCertificateTypes.class)).thenReturn(certificateTypes);

        BlankCredentialDistribution bcd = new BlankCredentialDistribution();
        bcd.setQuantity(5);
        bcd.setCredentialTypeCode("E");

        BlankCredentialDistribution res = this.restUtils.processBlankDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessBlankDistribution_OC_null() {
        final String credentialType = "OC";
        BlankDistributionSummaryDTO summary = new BlankDistributionSummaryDTO();
        summary.setCredentialType("OC");
        summary.setBatchId(4564L);

        GradCertificateTypes certificateTypes = new GradCertificateTypes();
        certificateTypes.setPaperType("YED2");
        certificateTypes.setCode("E");
        certificateTypes.setDescription("SDS");
        certificateTypes.setLabel("fere");

        when(this.restService.get(String.format(constants.getCertificateTypes(),"E"), GradCertificateTypes.class)).thenReturn(null);

        BlankCredentialDistribution bcd = new BlankCredentialDistribution();
        bcd.setQuantity(5);
        bcd.setCredentialTypeCode("E");

        BlankCredentialDistribution res = this.restUtils.processBlankDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessPsiDistribution() {
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen("1234567");
        scd.setPsiYear("2021");
        scd.setPsiCode("001");
        scd.setStudentID(UUID.randomUUID());
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen("2345678");
        bcd.setPsiCode("002");
        bcd.setPsiYear("2021");
        bcd.setStudentID(UUID.randomUUID());

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessPsiDistribution_else() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "1232131231";
        final String pen2 = "12321312";
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen(pen);
        scd.setPsiYear("2021");
        scd.setStudentID(studentID);
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen(pen2);
        bcd.setPsiCode("001");
        bcd.setPsiYear("2021");

        final Student student = new Student();
        student.setStudentID(studentID.toString());
        student.setPen(pen2);

        when(this.restService.get(String.format(constants.getPenStudentApiByPenUrl(), pen2), List.class)).thenReturn(Arrays.asList(student));

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessPsiDistribution_else_2() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "1232131231";
        final String pen2 = "12321312";
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen(pen);
        scd.setPsiYear("2021");
        scd.setStudentID(studentID);
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen(pen2);
        bcd.setPsiCode("001");
        bcd.setPsiYear("2021");

        when(this.restService.get(String.format(constants.getPenStudentApiByPenUrl(), pen2), List.class)).thenReturn(new ArrayList<>());

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
    }

    @Test
    public void testProcessPsiDistribution_Exception() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "1232131231";
        final String pen2 = "12321312";
        final Long batchId = 9879L;
        List<PsiCredentialDistribution> globalList = new ArrayList<>();

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setPen(pen);
        scd.setPsiYear("2021");
        scd.setStudentID(studentID);
        globalList.add(scd);

        PsiDistributionSummaryDTO summary = new PsiDistributionSummaryDTO();
        summary.setBatchId(batchId);
        summary.setGlobalList(globalList);

        PsiCredentialDistribution bcd = new PsiCredentialDistribution();
        bcd.setPen(pen2);
        bcd.setPsiCode("001");
        bcd.setPsiYear("2021");
        bcd.setStudentID(studentID);

        final Student student = new Student();
        student.setStudentID(studentID.toString());
        student.setPen(pen2);

        when(this.restService.get(String.format(constants.getPenStudentApiByPenUrl(), pen2), List.class)).thenThrow(new RuntimeException("Unable to retrieve PEN from PEN-API"));

        PsiCredentialDistribution res = this.restUtils.processPsiDistribution(bcd,summary);
        assertNotNull(res);
        assertThat(summary.getErrors()).isNotEmpty();
    }

    @Test
    public void testCreateBlankCredentialsAndUpload() {
        final Long batchId = 9879L;

        DistributionResponse res = new DistributionResponse();
        res.setMergeProcessResponse("Done");

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).build();

        when(this.restService.post(String.format(constants.getCreateBlanksAndUpload(),batchId,"N"), distributionRequest, DistributionResponse.class, "accessToken")).thenReturn(res);

        this.restUtils.createBlankCredentialsAndUpload(batchId,"accessToken",distributionRequest,"N");
        assertNotNull(res);
    }

    @Test
    public void testCreateBlankCredentialsAndUpload_null() {
        final Long batchId = 9879L;

        DistributionResponse res = new DistributionResponse();
        res.setMergeProcessResponse("Done");

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).build();

        when(this.restService.post(String.format(constants.getCreateBlanksAndUpload(),batchId,"N"), distributionRequest, DistributionResponse.class, "accessToken")).thenReturn(null);

        this.restUtils.createBlankCredentialsAndUpload(batchId,"accessToken",distributionRequest,"N");
        assertNotNull(res);
    }

    @Test
    public void testcreateAndStoreSchoolReports_null() {
        final String type = "NONGRADPRJ";

        when(this.restService.post(String.format(constants.getCreateAndStoreSchoolReports(),type), new ArrayList<>(), Integer.class)).thenReturn(null);

        mockTokenResponseObject();

        var result = this.restUtils.createAndStoreSchoolReports(new ArrayList<>(),type);
        assertNotNull(type);
        assertNotNull(result);
    }

    @Test
    public void testcreateAndStoreSchoolReports() {
        final String type = "NONGRADPRJ";

        when(this.restService.post(String.format(constants.getCreateAndStoreSchoolReports(),type), new ArrayList<>(), Integer.class)).thenReturn(1);

        mockTokenResponseObject();

        var result = this.restUtils.createAndStoreSchoolReports(new ArrayList<>(),type);
        assertNotNull(type);
        assertNotNull(result);
    }

    @Test
    public void testProcessStudentReports() {
        final String studentReportType = "TVRRUN";
        UUID studentID = UUID.randomUUID();

        when(this.restService.post(String.format(constants.getUpdateStudentReport(),studentReportType), List.of(studentID), Integer.class, "accessToken")).thenReturn(1);

        mockTokenResponseObject();

        var result = this.restUtils.processStudentReports(List.of(studentID),studentReportType);
        assertNotNull(studentReportType);
        assertNotNull(result);
    }

    @Test
    public void testCreateAndStoreSchoolReports_0() {
        final String type = "NONGRADPRJ";

        when(this.restService.post(String.format(constants.getCreateAndStoreSchoolReports(),type), new ArrayList<>(), Integer.class)).thenReturn(0);

        mockTokenResponseObject();

        var result = this.restUtils.createAndStoreSchoolReports(new ArrayList<>(),type);
        assertNotNull(type);
        assertNotNull(result);
    }

    @Test
    public void testCreateAndStoreSchoolReports_WithParams_ThenReturnResult() {
        final String type = "NONGRADPRJ";
        final UUID schoolId = UUID.randomUUID();

        when(this.restService.post(String.format(constants.getCreateAndStoreSchoolReports(),type), List.of(schoolId), Integer.class)).thenReturn(2);
        when(LOGGER.isDebugEnabled()).thenReturn(true);

        mockTokenResponseObject();

        var result = this.restUtils.createAndStoreSchoolReports(List.of(schoolId),type);
        assertNotNull(type);
        assertNotNull(result);
    }

    @Test
    public void whenCreateAndStoreSchoolReports_WithParams_ThenReturnResult() {
        final String type = "TVRRUN";
        final UUID schoolId = UUID.randomUUID();

        when(this.restService.post(String.format(constants.getCreateAndStoreSchoolReports(),type), List.of(schoolId), Integer.class)).thenReturn(2);
        when(LOGGER.isDebugEnabled()).thenReturn(true);

        mockTokenResponseObject();

        var result = this.restUtils.createAndStoreSchoolReports(schoolId, type, new SchoolReportsRegenSummaryDTO());
        assertNotNull(type);
        assertNotNull(result);
    }

    @Test
    public void whenCreateAndStoreSchoolReports_WithParams_ThenReturnsEmptyList() {
        final String type = "TVRRUN";

        when(LOGGER.isDebugEnabled()).thenReturn(true);

        var result = this.restUtils.createAndStoreSchoolReports(null, type, new SchoolReportsRegenSummaryDTO());
        assertThat(result).isZero();
    }

    @Test
    public void whenCreateAndStoreSchoolReports_WithParams_ThenThrowException() {
        final String type = "TVRRUN";
        final UUID schoolId = UUID.randomUUID();

        SchoolReportsRegenSummaryDTO summaryDTO = new SchoolReportsRegenSummaryDTO();

        when(this.restService.post(String.format(constants.getCreateAndStoreSchoolReports(),type), List.of(schoolId), Integer.class)).thenThrow(RuntimeException.class);
        when(LOGGER.isDebugEnabled()).thenReturn(true);

        val result = this.restUtils.createAndStoreSchoolReports(schoolId, type, summaryDTO);
        assertThat(result).isZero();
    }

    @Test
    public void testGetSchoolReportsLiteByReportType() {

        UUID uuid = UUID.randomUUID();
        SchoolReport sr = new SchoolReport();
        sr.setId(uuid);
        sr.setReportTypeCode("GRADREG");
        List<SchoolReport> schoolReportsLite = new ArrayList<>();
        schoolReportsLite.add(sr);

        mockTokenResponseObject();
        when(this.restService.get(String.format(constants.getSchoolReportsLiteByReportTypeUrl(), "GRADREG"), List.class)).thenReturn(schoolReportsLite);
        when(LOGGER.isDebugEnabled()).thenReturn(true);

        val result = this.restUtils.getSchoolReportsLiteByReportType("GRADREG", new SchoolReportsRegenSummaryDTO());
        assertThat(result).hasSize(1);
    }

    @Test
    public void whenGetSchoolReportsLiteByReportType_ThenThrowException() {
        UUID uuid = UUID.randomUUID();
        SchoolReport sr = new SchoolReport();
        sr.setId(uuid);
        sr.setReportTypeCode("GRADREG");
        List<SchoolReport> schoolReportsLite = new ArrayList<>();
        schoolReportsLite.add(sr);

        mockTokenResponseObject();

        when(this.restService.get(String.format(constants.getSchoolReportsLiteByReportTypeUrl(), "GRADREG"), List.class)).thenThrow(new RuntimeException("Test Exception"));
        when(LOGGER.isDebugEnabled()).thenReturn(true);

        val result = this.restUtils.getSchoolReportsLiteByReportType("GRADREG", new SchoolReportsRegenSummaryDTO());
        assertThat(result).isEmpty();
    }

    @Test
    public void testRunGradAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        String programCompletionDate = "2020/01";

        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        AlgorithmResponse res = new AlgorithmResponse();
        res.setGraduationStudentRecord(grd);
        res.setStudentOptionalProgram(new ArrayList<>());

        when(this.restService.get(String.format(constants.getGraduationApiReportOnlyUrl(), studentID,null), AlgorithmResponse.class)).thenReturn(res);

        val result = this.restUtils.runGradAlgorithm(UUID.fromString(studentID), grd.getProgram(), programCompletionDate,null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testRunGradAlgorithm_programCompletionDateNull() {
        final String studentID = UUID.randomUUID().toString();
        final Student student = new Student();
        String programCompletionDate = "2020/01";

        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        AlgorithmResponse res = new AlgorithmResponse();
        res.setGraduationStudentRecord(grd);
        res.setStudentOptionalProgram(new ArrayList<>());

        when(this.restService.get(String.format(constants.getGraduationApiUrl(), studentID,null), AlgorithmResponse.class)).thenReturn(res);

        val result = this.restUtils.runGradAlgorithm(UUID.fromString(studentID), grd.getProgram(),null,null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testRunProjectedGradAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");
        AlgorithmResponse res = new AlgorithmResponse();
        res.setGraduationStudentRecord(grd);
        res.setStudentOptionalProgram(new ArrayList<>());

        when(this.restService.get(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,null), AlgorithmResponse.class)).thenReturn(res);

        val result = this.restUtils.runProjectedGradAlgorithm(UUID.fromString(studentID),null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetStudentsForAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        mockTokenResponseObject();

        when(this.restService.get(constants.getGradStudentApiStudentForGradListUrl(), List.class)).thenReturn(Arrays.asList(grd.getStudentID()));

        val result = this.restUtils.getStudentsForAlgorithm();
        assertThat(result).isNotNull();
        assertThat(result.size()).isPositive();
    }

    @Test
    public void testGetStudentsForProjectedAlgorithm() {
        final String studentID = UUID.randomUUID().toString();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        mockTokenResponseObject();

        when(this.restService.get(constants.getGradStudentApiStudentForProjectedGradListUrl(), List.class)).thenReturn(Arrays.asList(grd.getStudentID()));

        val result = this.restUtils.getStudentsForProjectedAlgorithm();
        assertThat(result).isNotNull();
        assertThat(result.size()).isPositive();
    }

    @Test
    public void testGetStudentForBatchInput() {
        final UUID schoolId = UUID.randomUUID();
        final UUID studentID = UUID.randomUUID();
        BatchGraduationStudentRecord grd = new BatchGraduationStudentRecord(studentID, "2018-EN", null, schoolId);

        when(this.restService.get(String.format(constants.getGradStudentApiGradStatusForBatchUrl(), studentID), BatchGraduationStudentRecord.class)).thenReturn(grd);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();

        val result = this.restUtils.getStudentForBatchInput(studentID, summary);
        assertThat(result).isNotNull();
        assertThat(result.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testGetStudentForBatchInput_When_APIisDown_returns_null() {
        final UUID studentID = UUID.randomUUID();
        final UUID schoolId = UUID.randomUUID();
        BatchGraduationStudentRecord grd = new BatchGraduationStudentRecord(studentID, "2018-EN", null, schoolId);

        when(this.restService.get(String.format(constants.getGradStudentApiGradStatusForBatchUrl(), studentID), BatchGraduationStudentRecord.class)).thenReturn(grd);

        AlgorithmSummaryDTO summary = new AlgorithmSummaryDTO();
        summary.setAccessToken("123");

        when(this.restUtils.runGetStudentForBatchInput(studentID)).thenThrow(new RuntimeException("GRAD-STUDENT-API is down."));

        val result = this.restUtils.getStudentForBatchInput(studentID, summary);
        assertThat(result).isNull();
    }

    @Test
    public void testGetStudentDataForBatch() {
        final UUID studentID = UUID.randomUUID();
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(studentID);
        grd.setProgram("2018-EN");

        when(this.restService.get(String.format(constants.getStudentInfo(),studentID), GraduationStudentRecord.class)).thenReturn(grd);

        GraduationStudentRecord res = this.restUtils.getStudentDataForBatch(studentID.toString());
        assertThat(res).isNotNull();
        assertThat(res.getStudentID()).isEqualTo(studentID);
    }

    @Test
    public void testUpdateStudentCredentialRecord() {
        final String studentID = UUID.randomUUID().toString();
        String credentialTypeCode = "E";
        String paperType="YED2";
        String activityCode="USERDISTOC";
        String documentStatusCode="COMPL";
        GraduationStudentRecord grd = new GraduationStudentRecord();
        grd.setStudentID(new UUID(1,1));
        grd.setProgram("2018-EN");

        when(this.restService.get(String.format(constants.getUpdateStudentCredential(),studentID,credentialTypeCode,paperType,documentStatusCode,activityCode), Boolean.class)).thenReturn(true);

        this.restUtils.updateStudentCredentialRecord(UUID.fromString(studentID),credentialTypeCode,paperType,documentStatusCode,activityCode,"accessToken");
        assertThat(grd).isNotNull();
    }

    @Test
    public void testGetStudentsForUserReqDisRun() {
        String credentialType = "OT";
        UUID schoolId = UUID.randomUUID();
        StudentSearchRequest req = new StudentSearchRequest();
        List<UUID> sch = List.of(schoolId);
        req.setSchoolIds(sch);
        List<StudentCredentialDistribution> scdList = new ArrayList<>();
        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setSchoolId(schoolId);
        scd.setPaperType("YED2");
        scd.setCredentialTypeCode("E");
        scd.setId(new UUID(1,1));
        scdList.add(scd);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getStudentDataForUserReqDisRun(),credentialType), req, List.class, "accessToken")).thenReturn(scdList);

        val result = this.restUtils.getStudentsForUserReqDisRun(credentialType,req);
        assertThat(result).isNotNull();
        assertThat(result.size()).isPositive();
    }

    @Test
    public void testGetStudentsForUserReqDisRunWithNullDistributionDate() {
        String activityCode = "USERDISTRC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        List<StudentCredentialDistribution> scdList = new ArrayList<>();
        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setSchoolOfRecord("1212211");
        scd.setPaperType("YED2");
        scd.setCredentialTypeCode("E");
        scd.setId(new UUID(1,1));
        scdList.add(scd);

        mockTokenResponseObject();

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setActivityCode(activityCode);

        when(this.restService.post(String.format(constants.getStudentDataForUserReqDisRunWithNullDistributionDate(),activityCode), searchRequest, List.class, "accessToken")).thenReturn(scdList);

        val result = this.restUtils.getStudentsForUserReqDisRunWithNullDistributionDate(activityCode,searchRequest);
        assertThat(result).isNotNull();
    }

    @Test
    public void testCreateReprintAndUpload() {
        String activityCode = "USERDISTRC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).activityCode(activityCode).build();

        when(this.restService.post(String.format(constants.getReprintAndUpload(),batchId,activityCode,null), distributionRequest, DistributionResponse.class, null)).thenReturn(req);

        val result = this.restUtils.createReprintAndUpload(batchId,null, distributionRequest, activityCode,null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testCreateReprintAndUpload_null() {
        String activityCode = "USERDISTRC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).activityCode(activityCode).build();
        when(this.restService.post(String.format(constants.getReprintAndUpload(),batchId,activityCode,null), distributionRequest, DistributionResponse.class)).thenReturn(null);

        val result = this.restUtils.createReprintAndUpload(batchId,null, distributionRequest, activityCode,null);
        assertThat(result).isNull();
    }

    @Test
    public void testMergeAndUpload() {
        String activityCode = "USERDISTOC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).activityCode(activityCode).build();

        when(this.restService.post(String.format(constants.getMergeAndUpload(),batchId,activityCode,"Y"), distributionRequest, DistributionResponse.class, "accessToken")).thenReturn(req);
        when(this.restService.get(constants.getSchoolDistrictMonthReport(), Integer.class, "accessToken")).thenReturn(4);
        when(this.restService.get(constants.getSchoolDistrictYearEndReport(), Integer.class, "accessToken")).thenReturn(4);

        mockTokenResponseObject();

        val result = this.restUtils.mergeAndUpload(batchId, distributionRequest,activityCode,"Y");
        assertThat(result).isNotNull();
    }

    @Test
    public void testMergeAndUpload_null() {
        String activityCode = "USERDISTOC";
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).activityCode(activityCode).build();

        when(this.restService.post(String.format(constants.getMergeAndUpload(),batchId,activityCode,"Y"), distributionRequest, DistributionResponse.class)).thenReturn(null);

        mockTokenResponseObject();

        val result = this.restUtils.mergeAndUpload(batchId, distributionRequest,activityCode,"Y");
        assertThat(result).isNull();
    }


    @Test
    public void testMergePSIAndUpload() {
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;
        //Grad2-1931
        String transmissionType = "ftp";

        mockTokenResponseObject();

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).build();
        when(this.restService.post(String.format(constants.getMergePsiAndUpload(),batchId,"Y"), distributionRequest, DistributionResponse.class)).thenReturn(req);

        val result = this.restUtils.mergePsiAndUpload(batchId,null, distributionRequest,"Y", transmissionType);
        assertThat(result).isNotNull();
    }

    @Test
    public void testMergePSIAndUpload_null() {
        DistributionResponse req = new DistributionResponse();
        req.setMergeProcessResponse("Merged");
        Long batchId = 3344L;
        String transmissionType = "ftp";

        DistributionRequest distributionRequest = DistributionRequest.builder().mapDist(new HashMap<>()).build();
        when(this.restService.post(String.format(constants.getMergePsiAndUpload(),batchId,"Y"), distributionRequest, DistributionResponse.class)).thenReturn(null);

        val result = this.restUtils.mergePsiAndUpload(batchId,null, distributionRequest,"Y",transmissionType);
        assertThat(result).isNotNull();
    }

    @Test
    public void testGetStudentData() {
        final UUID studentID = UUID.randomUUID();
        GraduationStudentRecordDistribution grd = new GraduationStudentRecordDistribution();
        grd.setStudentID(studentID);
        grd.setProgram("2018-EN");

        mockTokenResponseObject();

        when(this.restService.get(String.format(constants.getStudentInfo(),studentID), GraduationStudentRecordDistribution.class)).thenReturn(grd);

        GraduationStudentRecordDistribution res = this.restUtils.getStudentData(studentID.toString());
        assertThat(res).isNotNull();
    }

    @Test
    public void testGetDistrictsBySchoolCategoryCode() {
        ca.bc.gov.educ.api.batchgraduation.model.institute.District district = new ca.bc.gov.educ.api.batchgraduation.model.institute.District();
        district.setDistrictId(UUID.randomUUID().toString());
        district.setDistrictNumber("042");

        when(this.restService.get(String.format(constants.getDistricstBySchoolCategory(), "02"), List.class)).thenReturn(List.of(district));

        List<ca.bc.gov.educ.api.batchgraduation.model.institute.District> res = this.restUtils.getDistrictsBySchoolCategoryCode("02");
        assertThat(res).isNotNull();
    }

    @Test
    public void testGetDistrictsBySchoolCategoryCode_whenException_isThrown() {
        when(this.restService.get(String.format(constants.getDistricstBySchoolCategory(), "02"), List.class)).thenThrow(new RuntimeException("Test Exception"));

        List<ca.bc.gov.educ.api.batchgraduation.model.institute.District> res = this.restUtils.getDistrictsBySchoolCategoryCode("02");
        assertThat(res).isEmpty();
    }

    @Test
    public void testGetSchoolsBySchoolCategoryCode() {
        ca.bc.gov.educ.api.batchgraduation.model.institute.School school = new ca.bc.gov.educ.api.batchgraduation.model.institute.School();
        school.setSchoolId(UUID.randomUUID().toString());
        school.setDistrictId(UUID.randomUUID().toString());
        school.setMincode("1234567");

        when(this.restService.get(String.format(constants.getSchoolsBySchoolCategory(), "02"), List.class)).thenReturn(List.of(school));

        List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> res = this.restUtils.getSchoolsBySchoolCategoryCode("02");
        assertThat(res).isNotNull();
    }

    @Test
    public void testGetSchoolsBySchoolCategoryCode_whenException_isThrown() {
        when(this.restService.get(String.format(constants.getSchoolsBySchoolCategory(), "02"), List.class)).thenThrow(new RuntimeException("Test Exception"));

        List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> res = this.restUtils.getSchoolsBySchoolCategoryCode("02");
        assertThat(res).isEmpty();
    }

    @Test
    public void testSearchSchoolsByDistrictId() {
        UUID schoolId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();

        ca.bc.gov.educ.api.batchgraduation.model.institute.School school = new ca.bc.gov.educ.api.batchgraduation.model.institute.School();
        school.setSchoolId(schoolId.toString());
        school.setDistrictId(districtId.toString());
        school.setMincode("1234567");

        when(this.restService.get(String.format(constants.getSearchSchoolsByDistrictId(), districtId), List.class)).thenReturn(List.of(school));

        List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> res = this.restUtils.getSchoolsByDistrictId(districtId);
        assertThat(res).isNotNull();
    }

    @Test
    public void testSearchSchoolsByDistrictId_whenException_isThrown() {
        UUID districtId = UUID.randomUUID();
        when(this.restService.get(String.format(constants.getSearchSchoolsByDistrictId(), districtId), List.class)).thenThrow(new RuntimeException("Test Exception"));

        List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> res = this.restUtils.getSchoolsByDistrictId(districtId);
        assertThat(res).isEmpty();
    }

    @Test
    public void testSearchSchoolsByDistrictNumber() {
        UUID schoolId = UUID.randomUUID();
        UUID districtId = UUID.randomUUID();
        String districtNumber = "039";

        ca.bc.gov.educ.api.batchgraduation.model.institute.School school = new ca.bc.gov.educ.api.batchgraduation.model.institute.School();
        school.setSchoolId(schoolId.toString());
        school.setDistrictId(districtId.toString());
        school.setMincode("1234567");

        when(this.restService.get(String.format(constants.getSearchSchoolsByDistrictNumber(), districtNumber), List.class)).thenReturn(List.of(school));

        List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> res = this.restUtils.getSchoolsByDistrictNumber(districtNumber);
        assertThat(res).isNotNull();
    }

    @Test
    public void testSearchSchoolsByDistrictNumber_whenException_isThrown() {
        String districtNumber = "039";
        when(this.restService.get(String.format(constants.getSearchSchoolsByDistrictNumber(), districtNumber), List.class)).thenThrow(new RuntimeException("Test Exception"));

        List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> res = this.restUtils.getSchoolsByDistrictNumber(districtNumber);
        assertThat(res).isEmpty();
    }

    @Test
    public void testGetSchoolClob() {
        UUID schoolId = UUID.randomUUID();

        SchoolClob school = new SchoolClob();
        school.setSchoolId(schoolId.toString());
        school.setMinCode("1234567");

        when(this.restService.get(String.format(constants.getSchoolClobBySchoolId(), schoolId), SchoolClob.class)).thenReturn(school);

        val res = this.restUtils.getSchoolClob(schoolId);
        assertThat(res).isNotNull();
        assertThat(res.getSchoolId()).isEqualTo(schoolId.toString());
    }

    @Test
    public void testSearchSchoolClob() {
        UUID schoolId = UUID.randomUUID();
        String mincode = "12345678";

        SchoolClob school = new SchoolClob();
        school.setSchoolId(schoolId.toString());
        school.setMinCode(mincode);

        when(this.restService.get(String.format(constants.getSearchSchoolClobByMinCode(), mincode), SchoolClob.class)).thenReturn(school);

        val res = this.restUtils.getSchoolClob(mincode);
        assertThat(res).isNotNull();
        assertThat(res.getSchoolId()).isEqualTo(schoolId.toString());
        assertThat(res.getMinCode()).isEqualTo(mincode);
    }


    @Test
    public void testExecutePostDistribution() {
        DistributionResponse distributionResponse = new DistributionResponse();

        mockTokenResponseObject();
        when(this.restService.post(constants.getPostingDistribution(),distributionResponse, Boolean.class, "accessToken")).thenReturn(true);

        Boolean res = this.restUtils.executePostDistribution(distributionResponse);
        assertThat(res).isTrue();
    }

    @Test
    public void testupdateStudentGradRecord() {
        final UUID studentID = UUID.randomUUID();
        final String activityCode = "USERDISOC";
        final Long batchId = 4567L;

        mockTokenResponseObject();

        GraduationStudentRecord rec = new GraduationStudentRecord();
        rec.setStudentID(studentID);

        when(this.restService.post(String.format(constants.getUpdateStudentRecord(),studentID,batchId,activityCode),"{}", GraduationStudentRecord.class)).thenReturn(rec);

        this.restUtils.updateStudentGradRecord(studentID,batchId,activityCode);
        assertNotNull(rec);

    }

    @Test
    public void testUpdateStudentGradRecordHistory() {
        final UUID studentID = UUID.randomUUID();
        final String userName = "abc";
        final String accessToken = "xyz";
        final Long batchId = 4567L;

        GraduationStudentRecord rec = new GraduationStudentRecord();
        rec.setStudentID(studentID);

        when(this.restService.put(String.format(constants.getUpdateStudentRecordHistory(),studentID, batchId, userName),"{}", GraduationStudentRecord.class)).thenReturn(rec);

        this.restUtils.updateStudentGradRecordHistory(List.of(), batchId, accessToken, userName);

        when(this.restService.put(String.format(constants.getUpdateStudentRecordHistory(), batchId, userName, "USERSTUDARC"),"{}", GraduationStudentRecord.class)).thenReturn(new GraduationStudentRecord());

        mockTokenResponseObject();

        this.restUtils.updateStudentGradRecordHistory(List.of(studentID), batchId, userName, "USERSTUDARC");
        assertNotNull(rec);

    }

    @Test
    public void testUpdateSchoolReportRecord() {
        final String mincode = "123213123";
        String reportTypeCode = "E";

        when(this.restService.get(String.format(constants.getUpdateSchoolReport(),mincode,reportTypeCode), Boolean.class)).thenReturn(true);

        mockTokenResponseObject();

        restUtils.updateSchoolReportRecord(mincode,reportTypeCode,null);
        assertThat(reportTypeCode).isEqualTo("E");

        when(this.restService.delete(String.format(constants.getUpdateSchoolReport(),mincode,reportTypeCode), Boolean.class)).thenReturn(true);

        restUtils.deleteSchoolReportRecord(mincode,reportTypeCode);
        assertThat(reportTypeCode).isEqualTo("E");
    }

    @Test
    public void testDeleteSchoolReportRecord() {
        final String mincode = "123213123";
        String reportTypeCode = "E";

        mockTokenResponseObject();

        when(this.restService.delete(String.format(constants.getUpdateSchoolReport(),mincode,reportTypeCode), Boolean.class)).thenReturn(true);

        this.restUtils.deleteSchoolReportRecord(mincode,reportTypeCode);
        assertThat(reportTypeCode).isEqualTo("E");
    }

    @Test
    public void testGetStudentByPenFromStudentAPI() {
        final UUID studentID = UUID.randomUUID();
        final UUID schoolId = UUID.randomUUID();

        final String pen = "123456789";
        final String mincode = "12345678";

        List<LoadStudentData> loadStudentData = new ArrayList<>();
        LoadStudentData lSD = new LoadStudentData();
        lSD.setPen(pen);
        lSD.setStudentGrade("12");
        lSD.setHonoursStanding("N");
        lSD.setProgramCode("2018-EN");
        loadStudentData.add(lSD);

        final Student student = new Student();
        student.setStudentID(studentID.toString());
        student.setPen(pen);
        student.setMincode(mincode);

        when(this.restService.get(String.format(constants.getPenStudentApiByPenUrl(), pen), List.class)).thenReturn(List.of(student));

        mockTokenResponseObject();

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);
        graduationStatus.setSchoolOfRecordId(schoolId);

        SchoolClob school = new SchoolClob();
        school.setSchoolId(schoolId.toString());
        school.setMinCode(mincode);

        when(this.restService.get(String.format(constants.getSearchSchoolClobByMinCode(), mincode), SchoolClob.class)).thenReturn(school);
        when(this.restService.post(String.format(constants.getGradStudentApiGradStatusUrl(), studentID), loadStudentData, GraduationStudentRecord.class)).thenReturn(graduationStatus);

        Integer res = this.restUtils.getStudentByPenFromStudentAPI(loadStudentData);
        assertThat(res).isEqualTo(1);
    }

    @Test
    public void testUpdateStudentFlagReadyForBatch() {
        final UUID studentID = UUID.randomUUID();
        final String pen = "123456789";
        final String batchJobType = "REGALG";

        GraduationStudentRecord graduationStatus = new GraduationStudentRecord();
        graduationStatus.setStudentID(studentID);
        graduationStatus.setPen(pen);

        List<UUID> studentIDs = Arrays.asList(studentID);

        StudentList stuList = new StudentList();
        stuList.setStudentids(studentIDs);

        when(this.restService.post(String.format(constants.getUpdateStudentFlagReadyForBatchByStudentIDs(), batchJobType), stuList, String.class)).thenReturn("SUCCESS");

        var result = this.restUtils.updateStudentFlagReadyForBatch(studentIDs, batchJobType);
        assertThat(stuList).isNotNull();
        assertThat(result).isEqualTo("SUCCESS");
    }

    @Test
    public void testIsReportOnly_when_programCompletionDate_isInFuture_thenReturns_GS() {
        final UUID studentID = UUID.randomUUID();
        final String gradProgram = "SCCP";

        Date futureDate = DateUtils.addMonths(new Date(), 1);
        final String programCompletionDate = EducGradBatchGraduationApiUtils.formatDate(futureDate, "yyyy/MM");

        String url = constants.getCheckSccpCertificateExists() + "?studentID=%s";

        when(this.restService.get(String.format(url, studentID), Boolean.class)).thenReturn(true);

        val result = this.restUtils.isReportOnly(studentID, gradProgram, programCompletionDate);
        assertThat(result).isFalse();
    }

    @Test
    public void testIsReportOnly_when_programCompletionDate_isNotInFuture_and_SCCPcertificateExists_thenReturns_FMR() {
        final UUID studentID = UUID.randomUUID();
        final String gradProgram = "SCCP";
        final String programCompletionDate = "2023/01";

        String url = constants.getCheckSccpCertificateExists() + "?studentID=%s";

        when(this.restService.get(String.format(url, studentID), Boolean.class)).thenReturn(true);

        val result = this.restUtils.isReportOnly(studentID, gradProgram, programCompletionDate);
        assertThat(result).isTrue();
    }

    @Test
    public void testRunRegenerateStudentCertificates() {
        final String pen = "123456789";

        mockTokenResponseObject();

        String url = constants.getStudentCertificateRegeneration();
        url = url + "?isOverwrite=%s";
        when(this.restService.get(String.format(url, pen, "N"), Integer.class)).thenReturn(1);

        val result = this.restUtils.runRegenerateStudentCertificate(pen);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testFetchDistributionRequiredDataStudentsNonGradYearly() {
        ReportGradStudentData reportGradStudentData = new ReportGradStudentData();

        mockTokenResponseObject();

        when(this.restService.get(String.format(constants.getStudentDataNonGradEarly()), List.class, "accessToken")).thenReturn(List.of(reportGradStudentData));

        val result = this.restUtils.fetchDistributionRequiredDataStudentsNonGradYearly();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testFetchDistributionRequiredDataStudentsNonGradYearlyBySchoolId() {
        UUID schoolId = UUID.randomUUID();
        ReportGradStudentData reportGradStudentData = new ReportGradStudentData();

        mockTokenResponseObject();

        when(this.restService.get(String.format(constants.getStudentDataNonGradEarlyBySchoolId(), schoolId), List.class, "accessToken")).thenReturn(List.of(reportGradStudentData));

        val result = this.restUtils.fetchDistributionRequiredDataStudentsNonGradYearly(schoolId);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testFetchDistributionRequiredDataStudentsYearly() {
        ReportGradStudentData reportGradStudentData = new ReportGradStudentData();

        mockTokenResponseObject();

        when(this.restService.get(constants.getStudentReportDataYearly(), List.class, "accessToken")).thenReturn(List.of(reportGradStudentData));

        val result = this.restUtils.fetchDistributionRequiredDataStudentsYearly();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testGetEDWSnapshotSchools() {
        final Integer gradYear = Integer.parseInt("2023");

        List<String> schools = Arrays.asList("12345678","11223344");

        mockTokenResponseObject();

        when(this.restService.get(String.format(constants.getEdwSnapshotSchoolsUrl(), gradYear), List.class, "accessToken")).thenReturn(schools);

        val result = this.restUtils.getEDWSnapshotSchools(gradYear);
        assertThat(result).hasSize(2);
    }

    @Test
    public void testGetTotalSchoolReportsForArchiving() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradSchoolReportsCountUrl(), "GRADREG"), schools, Long.class)).thenReturn(1L);

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getTotalReportsForProcessing(schools, "GRADREG", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testGetTotalSchoolReportsRegeneration() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradSchoolReportsCountUrl(), "GRADREG"), schools, Long.class)).thenReturn(1L);

        SchoolReportsRegenSummaryDTO summaryDTO = new SchoolReportsRegenSummaryDTO();

        val result = this.restUtils.getTotalReportsForProcessing(schools, "GRADREG", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testGetReportStudentIDsByStudentIDsAndReportType() {
        UUID uuid = UUID.randomUUID();
        List<String> studentIDsIn = Arrays.asList(uuid.toString());
        List<UUID> studentIDsOut = Arrays.asList(uuid);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradStudentReportsGuidsUrl(), "ACHV", 1), studentIDsIn, List.class)).thenReturn(studentIDsOut);

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getReportStudentIDsByStudentIDsAndReportType(studentIDsIn, "ACHV", 1, summaryDTO);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testGetReportStudentIDsByStudentIDsAndReportTypeError() {
        UUID uuid = UUID.randomUUID();
        List<String> studentIDsIn = Arrays.asList(uuid.toString());
        List<UUID> studentIDsOut = Arrays.asList(uuid);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradStudentReportsGuidsUrl(), "ACHV", 1), studentIDsIn, List.class)).thenThrow(new RuntimeException("Unable to retrieve report student guids"));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getReportStudentIDsByStudentIDsAndReportType(studentIDsIn, "ACHV", 1, summaryDTO);
        assertThat(result).isEmpty();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetStudentIDsBySearchCriteriaOrAll() {
        List<UUID> studentIDs = Arrays.asList(UUID.randomUUID());

        mockTokenResponseObject();

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setStudentIDs(studentIDs);

        when(this.restService.post(constants.getGradGetStudentsBySearchCriteriaUrl(), searchRequest, List.class)).thenReturn(studentIDs);

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getStudentIDsBySearchCriteriaOrAll(searchRequest, summaryDTO);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void testGetStudentIDsBySearchCriteriaOrAllError() {
        List<UUID> studentIDs = Arrays.asList(UUID.randomUUID());

        mockTokenResponseObject();

        StudentSearchRequest searchRequest = new StudentSearchRequest();
        searchRequest.setStudentIDs(studentIDs);

        when(this.restService.post(constants.getGradGetStudentsBySearchCriteriaUrl(), searchRequest, List.class)).thenThrow(new RuntimeException("Unable to retrieve list of Students"));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getStudentIDsBySearchCriteriaOrAll(searchRequest, summaryDTO);
        assertThat(result).isEmpty();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetTotalSchoolReportsForArchivingError() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradSchoolReportsCountUrl(), "GRADREG"), schools, Long.class)).thenThrow(new RuntimeException("Unable to retrieve school reports counts"));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.getTotalReportsForProcessing(schools, "GRADREG", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetTotalSchoolReportsRegenerationError() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradSchoolReportsCountUrl(), "GRADREG"), schools, Long.class)).thenThrow(new RuntimeException("Unable to retrieve school reports counts"));

        SchoolReportsRegenSummaryDTO summaryDTO = new SchoolReportsRegenSummaryDTO();

        val result = this.restUtils.getTotalReportsForProcessing(schools, "GRADREG", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testDeleteStudentReports() {
        List<UUID> studentIDs = Arrays.asList(UUID.randomUUID());

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getDeleteStudentReportsUrl(), 12345678L, "ACHV"), studentIDs, Long.class)).thenReturn(1L);

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.deleteStudentReports(12345678L, studentIDs,"ACHV", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testDeleteStudentReportsError() {
        List<UUID> studentIDs = Arrays.asList(UUID.randomUUID());

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getDeleteStudentReportsUrl(), 12345678L, "ACHV"), studentIDs, Long.class)).thenThrow(new RuntimeException("Unable to delete student reports"));


        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.deleteStudentReports(12345678L, studentIDs,"ACHV", summaryDTO);
        assertThat(result).isZero();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testArchiveSchoolReports() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradArchiveSchoolReportsUrl(), 12345678L, "GRADREG"), schools, Integer.class)).thenReturn(1);

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.archiveSchoolReports(12345678L, schools,"GRADREG", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testArchiveSchoolReportsError() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        when(this.restService.post(String.format(constants.getGradArchiveSchoolReportsUrl(), 12345678L, "GRADREG"), schools, Integer.class)).thenThrow(new RuntimeException("Unable to archive School Reports"));

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        val result = this.restUtils.archiveSchoolReports(12345678L, schools,"GRADREG", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetTotalStudentsForArchiving() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        when(this.restService.post(String.format(constants.getGradStudentCountUrl(), "CUR"), schools, Long.class)).thenReturn(1L);

        val result = this.restUtils.getTotalStudentsBySchoolOfRecordIdAndStudentStatus(schools, "CUR", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testGetTotalStudentsForArchivingError() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();

        when(this.restService.post(String.format(constants.getGradStudentCountUrl(), "CUR"), schools, Long.class)).thenThrow(new RuntimeException("Unable to retrieve student counts"));

        val result = this.restUtils.getTotalStudentsBySchoolOfRecordIdAndStudentStatus(schools, "CUR", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testArchiveStudents() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
        summaryDTO.setUserName("USER");

        when(this.restService.post(String.format(constants.getGradArchiveStudentsUrl(), 12345678L, "CUR", "USER"), schools, Integer.class)).thenReturn(1);

        val result = this.restUtils.archiveStudents(12345678L, schools,"CUR", summaryDTO);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testArchiveStudentsError() {
        UUID schoolId1 = UUID.randomUUID();
        UUID schoolId2 = UUID.randomUUID();
        List<UUID> schools = Arrays.asList(schoolId1, schoolId2);

        mockTokenResponseObject();

        DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
        summaryDTO.setUserName("USER");

        when(this.restService.post(String.format(constants.getGradArchiveStudentsUrl(), 12345678L, "CUR", "USER"), schools, Integer.class)).thenThrow(new RuntimeException("Unable to archive Students"));

        val result = this.restUtils.archiveStudents(12345678L, schools,"CUR", summaryDTO);
        assertThat(result).isNotNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }

    @Test
    public void testGetEDWSnapshotStudents() {
        final Integer gradYear = Integer.parseInt("2023");
        final String mincode = "12345678";

        SnapshotResponse snapshotResponse = new SnapshotResponse();
        snapshotResponse.setPen("123456789");
        snapshotResponse.setSchoolOfRecord(mincode);
        snapshotResponse.setGraduatedDate("202306");
        snapshotResponse.setGpa(BigDecimal.valueOf(3.75));
        snapshotResponse.setHonourFlag("N");

        mockTokenResponseObject();

        when(this.restService.get(String.format(constants.getEdwSnapshotStudentsByMincodeUrl(), gradYear, mincode), List.class, "accessToken")).thenReturn(List.of(snapshotResponse));
        val result = this.restUtils.getEDWSnapshotStudents(gradYear, mincode);
        assertThat(result).hasSize(1);
    }

    @Test
    public void testProcessSnapshot() {
        final Integer gradYear = Integer.parseInt("2023");
        final String mincode = "12345678";

        EdwGraduationSnapshot snapshot = new EdwGraduationSnapshot();
        snapshot.setStudentID(UUID.randomUUID());
        snapshot.setPen("123456789");
        snapshot.setGradYear(gradYear);
        snapshot.setSchoolOfRecord(mincode);

        when(this.restService.post(constants.getSnapshotGraduationStatusForEdwUrl(), snapshot, EdwGraduationSnapshot.class)).thenReturn(snapshot);

        val result = this.restUtils.processSnapshot(snapshot, new EdwSnapshotSummaryDTO());
        assertThat(result).isNotNull();
        assertThat(result.getPen()).isEqualTo(snapshot.getPen());
    }

    @Test
    public void testProcessSnapshotException() {
        final Integer gradYear = Integer.parseInt("2023");
        final String mincode = "12345678";

        EdwGraduationSnapshot snapshot = new EdwGraduationSnapshot();
        snapshot.setStudentID(UUID.randomUUID());
        snapshot.setPen("123456789");
        snapshot.setGradYear(gradYear);
        snapshot.setSchoolOfRecord(mincode);

        when(this.restService.post(constants.getSnapshotGraduationStatusForEdwUrl(), snapshot, EdwGraduationSnapshot.class)).thenThrow(new RuntimeException("Snapshot is unavailable!"));

        EdwSnapshotSummaryDTO summaryDTO = new EdwSnapshotSummaryDTO();

        val result = this.restUtils.processSnapshot(snapshot, summaryDTO);
        assertThat(result).isNull();
        assertThat(summaryDTO.getErrors()).isNotEmpty();
    }


    @Test
    public void testGetDeceasedStudentIDs() {
        final UUID studentID1 = UUID.randomUUID();
        final UUID studentID2 = UUID.randomUUID();

        List<UUID> studentIDs = new ArrayList<>();
        studentIDs.add(studentID1);
        studentIDs.add(studentID2);

        when(this.restService.post(constants.getDeceasedStudentIDList(), studentIDs, List.class)).thenReturn(studentIDs);

        val result = this.restUtils.getDeceasedStudentIDs(studentIDs);
        assertThat(result).hasSize(2);
    }

    @Test
    public void testFetchAccessToken() {
        mockTokenResponseObject();
        String result = this.restUtils.fetchAccessToken();
        assertThat(result).isNotNull();
    }

    private String mockTokenResponseObject() {
        final ResponseObj tokenObject = new ResponseObj();
        String mockToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJtbUhsTG4tUFlpdTl3MlVhRnh5Yk5nekQ3d2ZIb3ZBRFhHSzNROTk0cHZrIn0.eyJleHAiOjE2NjMxODg1MzMsImlhdCI6MTY2MzE4ODIzMywianRpIjoiZjA2ZWJmZDUtMzRlMi00NjY5LTg0MDktOThkNTc3OGZiYmM3IiwiaXNzIjoiaHR0cHM6Ly9zb2FtLWRldi5hcHBzLnNpbHZlci5kZXZvcHMuZ292LmJjLmNhL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI4ZGFjNmM3Yy0xYjU5LTQ5ZDEtOTMwNC0wZGRkMTdlZGE0YWQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJncmFkLWFkbWluLWNsaWVudCIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cHM6Ly9kZXYuZ3JhZC5nb3YuYmMuY2EiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6IldSSVRFX1NUVURFTlQgR1JBRF9CVVNJTkVTU19SIENSRUFURV9TVFVERU5UX1hNTF9UUkFOU0NSSVBUX1JFUE9SVCBDUkVBVEVfR1JBRF9BU1NFU1NNRU5UX1JFUVVJUkVNRU5UX0RBVEEgUkVBRF9TVFVERU5UIFJFQURfU0NIT09MIGVtYWlsIHByb2ZpbGUiLCJjbGllbnRJZCI6ImdyYWQtYWRtaW4tY2xpZW50IiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJjbGllbnRIb3N0IjoiMTQyLjMxLjQwLjE1NiIsInByZWZlcnJlZF91c2VybmFtZSI6InNlcnZpY2UtYWNjb3VudC1ncmFkLWFkbWluLWNsaWVudCIsImNsaWVudEFkZHJlc3MiOiIxNDIuMzEuNDAuMTU2In0.AqSxYzfanjhxCEuxLVHcJWA528AglXezS0-6EBohLsAJ4W1prdcrcS7p6yv1mSBs9GEkCu7SZhjl97xWaNXf7Emd4O0ieawgfXhDdgCtWtpLc0X2NjRTcZmv9kCpr__LmX4Zl3temUShNLVsSI95iBD7GKQmx_qTMpf3fiXdmmBvpZIibEly9RBbrio5DirqdYKuj0CO3x7xruBdBQnutr_GK7_vkmpw-X4RAyxsCwxSDequot1cCgMcJvPb6SxOL0BHx01OjM84FPwf2DwDrLvhXXhh4KucykUJ7QfiA5unmlLQ0wfG-bBJDwpjlXazF8jOQNEcasABVTftW6s8NA";
        tokenObject.setAccess_token(mockToken);
        tokenObject.setRefresh_token("456");

        when(this.tokenUtils.getTokenResponseObject()).thenReturn(tokenObject);
        when(this.tokenUtils.fetchAccessToken()).thenReturn("accessToken");
        return mockToken;
    }
}
