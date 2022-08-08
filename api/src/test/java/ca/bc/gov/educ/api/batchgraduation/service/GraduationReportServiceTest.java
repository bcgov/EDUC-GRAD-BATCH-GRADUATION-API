package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchProcessingRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GraduationReportServiceTest {

    @Autowired
    GraduationReportService graduationReportService;

    @MockBean
    private RestUtils restUtils;

    @MockBean
    WebClient webClient;

    @Mock
    WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock WebClient.RequestHeadersUriSpec requestHeadersUriMock;
    @Mock WebClient.ResponseSpec responseMock;
    @Mock WebClient.RequestBodySpec requestBodyMock;
    @Mock WebClient.RequestBodyUriSpec requestBodyUriMock;

    @Autowired
    EducGradBatchGraduationApiConstants constants;

    @Test
    public void testGetTranscriptYearly() {

        StudentCredentialDistribution scd = new StudentCredentialDistribution();
        scd.setId(new UUID(1,1));
        scd.setStudentID(new UUID(2,2));
        scd.setCredentialTypeCode("E");
        scd.setPaperType("YED2");
        scd.setSchoolOfRecord("05005001");

        ParameterizedTypeReference<List<StudentCredentialDistribution>> tListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getTranscriptYearlyDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(List.of(scd)));

        List<StudentCredentialDistribution> res = graduationReportService.getTranscriptListYearly("accessTaken").block();
        assertThat(res).isNotNull().hasSize(1);

    }

    @Test
    public void testGetSchoolReportForPosting() {

        SchoolReportDistribution scd = new SchoolReportDistribution();
        scd.setId(new UUID(1,1));
        scd.setReportTypeCode("GRAD");
        scd.setSchoolOfRecord("05005001");

        ParameterizedTypeReference<List<SchoolReportDistribution>> tListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getSchoolReportPostingList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(List.of(scd)));

        List<SchoolReportDistribution> res = graduationReportService.getSchoolReportForPosting("accessToken");
        assertThat(res).isNotNull().hasSize(1);

    }

    @Test
    public void testGetPsiStudentsForRun() {

        String transmissionType="PAPER";
        String psiCode="001";
        String psiYear="2021";

        PsiCredentialDistribution scd = new PsiCredentialDistribution();
        scd.setStudentID(new UUID(2,2));
        scd.setPen("122312312");
        scd.setPsiCode("001");
        scd.setPsiYear("2021");

        ParameterizedTypeReference<List<PsiCredentialDistribution>> tListRes = new ParameterizedTypeReference<>() {
        };

        when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPsiStudentList(),transmissionType,psiCode,psiYear))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(List.of(scd)));

        List<PsiCredentialDistribution> res = graduationReportService.getPsiStudentsForRun(transmissionType,psiCode,psiYear,"accessToken");
        assertThat(res).isNotNull().hasSize(1);

    }



}
