package ca.bc.gov.educ.api.batchgraduation.rest;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;

@Component
public class RestUtils {

    private final EducGradBatchGraduationApiConstants constants;

    private final WebClient webClient;

    @Autowired
    public RestUtils(final EducGradBatchGraduationApiConstants constants, final WebClient webClient) {
        this.constants = constants;
        this.webClient = webClient;
    }

    public ResponseObj getTokenResponseObject() {
        HttpHeaders httpHeadersKC = EducGradBatchGraduationApiUtils.getHeaders(
                constants.getUserName(), constants.getPassword());
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        System.out.println("url = " + constants.getTokenUrl());
        return this.webClient.post().uri(constants.getTokenUrl())
                .headers(h -> h.addAll(httpHeadersKC))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .bodyToMono(ResponseObj.class).block();
    }

    public List<Student> getStudentsByPen(String pen, String accessToken) {
        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        System.out.println("url = " + constants.getPenStudentApiByPenUrl());
        return this.webClient.get()
                .uri(String.format(constants.getPenStudentApiByPenUrl(), pen))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }
    
    public AlgorithmResponse runGradAlgorithm(UUID studentID, String accessToken) {
        return this.webClient.get()
        		.uri(String.format(constants.getGraduationApiUrl(), studentID))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(AlgorithmResponse.class).block();
    }
    
    public List<GraduationStatus> getStudentsForAlgorithm(String accessToken) {
        final ParameterizedTypeReference<List<GraduationStatus>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradStudentApiStudentForGradListUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    // EDUC-GRAD-STUDENT-API ========================================
    public GraduationStatus getGraduationStatus(String pen, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradStudentApiGradStatusUrl(), uri -> uri.path("/pen/{pen}").build(pen))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(GraduationStatus.class).block();
    }

    public GraduationStatus saveGraduationStatus(GraduationStatus graduationStatus, String accessToken) {
        return this.webClient.post()
                .uri(constants.getGradStudentApiGradStatusUrl(), uri -> uri.path("/{studentID}").build(graduationStatus.getStudentID()))
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(graduationStatus))
                .retrieve().bodyToMono(GraduationStatus.class).block();
    }

}
