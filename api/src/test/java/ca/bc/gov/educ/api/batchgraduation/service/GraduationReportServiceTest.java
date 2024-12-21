package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.model.PsiCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.model.StudentCredentialDistribution;
import ca.bc.gov.educ.api.batchgraduation.rest.RESTService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
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
    RESTService restService;

    @MockBean
    @Qualifier("batchClient")
    WebClient batchWebClient;

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

        when(this.batchWebClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(constants.getTranscriptYearlyDistributionList())).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(List.of(scd)));

        List<StudentCredentialDistribution> res = graduationReportService.getTranscriptListYearly().block();
        assertThat(res).isNotNull().hasSize(1);

    }

    @Test
    public void testGetSchoolsNonGradYearly() {

        when(this.restService.get(constants.getSchoolDataNonGradEarly(), List.class, "accessToken")).thenReturn(List.of(UUID.randomUUID()));

        List<UUID> res = graduationReportService.getSchoolsNonGradYearly("accessToken");
        assertThat(res).isNotEmpty();

    }

    @Test
    public void testGetDistrictsNonGradYearly() {

        when(this.restService.get(constants.getDistrictDataNonGradEarly(), List.class, "accessToken")).thenReturn(List.of(UUID.randomUUID()));

        List<UUID> res = graduationReportService.getDistrictsNonGradYearly("accessToken");
        assertThat(res).isNotEmpty();

    }

    @Test
    public void testGetDistrictsYearly() {

        when(this.restService.get(constants.getDistrictDataYearly(), List.class, "accessToken")).thenReturn(List.of(UUID.randomUUID()));

        List<UUID> res = graduationReportService.getDistrictsYearly("accessToken");
        assertThat(res).isNotEmpty();

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

        when(this.batchWebClient.get()).thenReturn(this.requestHeadersUriMock);
        when(this.requestHeadersUriMock.uri(String.format(constants.getPsiStudentList(),transmissionType,psiCode,psiYear))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.headers(any(Consumer.class))).thenReturn(this.requestHeadersMock);
        when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
        when(this.responseMock.bodyToMono(tListRes)).thenReturn(Mono.just(List.of(scd)));

        List<PsiCredentialDistribution> res = graduationReportService.getPsiStudentsForRun(transmissionType,psiCode,psiYear,"accessToken");
        assertThat(res).isNotNull().hasSize(1);

    }



}
