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

    public Student getStudentByPen(String pen, String accessToken) {
        System.out.println("url = " + constants.getPenStudentApiByPenUrl());
        return this.webClient.get()
                .uri(String.format(constants.getPenStudentApiByPenUrl(), pen))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(Student.class).block();
    }

    public GradSpecialProgram getGradSpecialProgram(String programCode, String specialProgramCode, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradProgramManagementUrl(), uri -> uri.path("/{programCode}/{specialProgramCode}").build(programCode, specialProgramCode))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(GradSpecialProgram.class).block();
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
                .uri(constants.getGradStudentForGradListUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    // EDUC-GRAD-STUDENT-API
    public GraduationStatus getGraduationStatus(String pen, String accessToken) {
        return this.webClient.get()
                .uri(constants.getGradStudentUrl(), uri -> uri.path("/pen/{pen}").build(pen))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(GraduationStatus.class).block();
    }

    public GraduationStatus saveGraduationStatus(GraduationStatus graduationStatus, String accessToken) {
        return this.webClient.post()
                .uri(constants.getSaveGradStudentUrl(), uri -> uri.path("/{studentID}").build(graduationStatus.getStudentID()))
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(graduationStatus))
                .retrieve().bodyToMono(GraduationStatus.class).block();
    }

    // EDUC-GRAD-PROGRAM-API
    public GradStudentSpecialProgram getStudentSpecialProgram(UUID studentID, UUID specialProgramID, String accessToken) {
        return this.webClient.get()
                .uri(constants.getStudentSpecialProgramUrl(), uri -> uri.path("/{studentID}/{specialProgramID}")
                        .build(studentID, specialProgramID))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(GradStudentSpecialProgram.class).block();
    }

    public GradStudentSpecialProgram saveStudentSpecialProgram(GradStudentSpecialProgram gradStudentSpecialProgram, String accessToken) {
        return this.webClient.post()
                .uri(constants.getSaveStudentSpecialProgramUrl(), uri -> uri.path("/{studentID}/{specialProgramID}").build(gradStudentSpecialProgram.getId(), gradStudentSpecialProgram.getSpecialProgramID()))
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(gradStudentSpecialProgram))
                .retrieve().bodyToMono(GradStudentSpecialProgram.class).block();
    }

    // EDUC-GRAD-COURSE-API
    public Integer getCountOfFrenchImmersionCourses(String pen) {
        return 0;
    }

    // EDUC-GRAD-COURSE-API
    public GradCourseRestriction getCourseRestriction(String mainCourse, String mainCourseLevel, String restrictedCourse, String restrictedCourseLevel, String accessToken) {
        return this.webClient.get()
                .uri(constants.getCourseRestrictionUrl(), uri -> uri.path("/{mainCourse}/{mainCourseLevel}/{restrictedCourse}/{restrictedCourseLevel}")
                        .build(mainCourse, mainCourseLevel, restrictedCourse, restrictedCourseLevel))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(GradCourseRestriction.class).block();
    }

    public List<GradCourseRestriction> getCourseRestrictions(String mainCourse, String restrictedCourse, String accessToken) {
        final ParameterizedTypeReference<List<GradCourseRestriction>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getCourseRestrictionUrl(), uri -> uri.path("/{mainCourse}/{restrictedCourse}")
                        .build(mainCourse, restrictedCourse))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    public GradCourseRestriction saveCourseRestriction(GradCourseRestriction gradCourseRestriction, String accessToken) {
        return this.webClient.post()
                .uri(constants.getSaveCourseRestrictionUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(gradCourseRestriction))
                .retrieve().bodyToMono(GradCourseRestriction.class).block();
    }


}
