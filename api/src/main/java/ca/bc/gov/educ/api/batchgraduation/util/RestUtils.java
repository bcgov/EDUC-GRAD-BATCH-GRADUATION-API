package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.GradSpecialProgram;
import ca.bc.gov.educ.api.batchgraduation.model.ResponseObj;
import ca.bc.gov.educ.api.batchgraduation.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class RestUtils {

    private final WebClient webClient;

    @Value("${authorization.user}")
    private String uName;

    @Value("${authorization.password}")
    private String pass;

    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_GET_TOKEN_URL)
    private String getToken;

    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_PEN_STUDENT_API_BY_PEN_URL)
    private String getPenStudentAPIByPenURL;

    @Value(EducGradBatchGraduationApiConstants.ENDPOINT_GRAD_PROGRAM_MANAGEMENT_URL)
    private String getGradProgramManagementAPIForSpecialProgram;

    @Autowired
    public RestUtils(final WebClient webClient) {
        this.webClient = webClient;
    }

    public ResponseObj getTokenResponseObject() {
        HttpHeaders httpHeadersKC = EducGradBatchGraduationApiUtils.getHeaders(uName,pass);
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");

        return webClient.post().uri(getToken)
                .headers(h -> h.addAll(httpHeadersKC))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .bodyToMono(ResponseObj.class).block();
    }

    public List<Student> getStudentsByPen(String pen, String accessToken) {
        return webClient.get()
                .uri(String.format(getPenStudentAPIByPenURL, pen))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToFlux(Student.class).collectList().block();
    }

    public GradSpecialProgram getGradSpecialProgram(String programCode, String specialProgramCode, String accessToken) {
        return webClient.get()
                .uri(String.format(getGradProgramManagementAPIForSpecialProgram, programCode, specialProgramCode))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(GradSpecialProgram.class).block();
    }
}
