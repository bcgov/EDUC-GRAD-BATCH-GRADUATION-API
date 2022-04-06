package ca.bc.gov.educ.api.batchgraduation.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;

@Component
public class RestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestUtils.class);

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
    
    public AlgorithmResponse runGradAlgorithm(UUID studentID, String accessToken, String programCompleteDate,Long batchId) {
        if(programCompleteDate != null) {
            return this.webClient.get()
            		.uri(String.format(constants.getGraduationApiReportOnlyUrl(), studentID,batchId))
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve().bodyToMono(AlgorithmResponse.class).block();
        }
    	return this.webClient.get()
        		.uri(String.format(constants.getGraduationApiUrl(), studentID,batchId))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(AlgorithmResponse.class).block();
    }

    public AlgorithmResponse runProjectedGradAlgorithm(UUID studentID, String accessToken,Long batchId) {
        return this.webClient.get()
            .uri(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId))
            .headers(h -> h.setBearerAuth(accessToken))
            .retrieve().bodyToMono(AlgorithmResponse.class).block();

    }
    
    public List<GraduationStudentRecord> getStudentsForAlgorithm(String accessToken) {
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradStudentApiStudentForGradListUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    public List<GraduationStudentRecord> getStudentsForProjectedAlgorithm(String accessToken) {
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradStudentApiStudentForProjectedGradListUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    // EDUC-GRAD-STUDENT-API ========================================

    public GraduationStudentRecord saveGraduationStudentRecord(GraduationStudentRecord graduationStudentRecord, String accessToken) {
        return this.webClient.post()
                .uri(String.format(constants.getGradStudentApiGradStatusUrl(),graduationStudentRecord.getStudentID()))
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(graduationStudentRecord))
                .retrieve().bodyToMono(GraduationStudentRecord.class).block();
    }

    public List<GraduationStudentRecord> getStudentsForSpecialGradRun(StudentSearchRequest req,String accessToken) {
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        GraduationStudentRecordSearchResult res = this.webClient.post()
                .uri(constants.getGradStudentApiStudentForSpcGradListUrl())
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(req))
                .retrieve()
                .bodyToMono(GraduationStudentRecordSearchResult.class)
                .block();
        return res.getGraduationStudentRecords();
    }

    public GraduationStudentRecord processStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
        LOGGER.info("*** {} Partition  - Processing  * STUDENT ID: * {}",Thread.currentThread().getName(),item.getStudentID().toString());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            AlgorithmResponse algorithmResponse = this.runGradAlgorithm(item.getStudentID(), accessToken,item.getProgramCompletionDate(),summary.getBatchId());
            if(algorithmResponse.getException() != null) {
                ProcessError error = new ProcessError();
                error.setStudentID(item.getStudentID().toString());
                error.setReason(algorithmResponse.getException().getExceptionName());
                error.setDetail(algorithmResponse.getException().getExceptionDetails());
                summary.getErrors().add(error);
                summary.setProcessedCount(summary.getProcessedCount() - 1L);
                return null;
            }
            LOGGER.info("*** {} Partition  * Processed student[{}] * Student ID: {} in total {}",Thread.currentThread().getName(), summary.getProcessedCount(), item.getStudentID(), summary.getReadCount());
            return algorithmResponse.getGraduationStudentRecord();
        }catch(Exception e) {
            ProcessError error = new ProcessError();
            error.setStudentID(item.getStudentID().toString());
            error.setReason("GRAD-GRADUATION-API IS DOWN");
            error.setDetail("Graduation API is unavialble at this moment");
            summary.getErrors().add(error);
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info("*** {} Partition  - Processing Failed  * STUDENT ID: * {}",Thread.currentThread().getName(),item.getStudentID().toString());
            return null;
        }

    }

    public GraduationStudentRecord processProjectedGradStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
        LOGGER.info("*** {} Partition  - Processing  * STUDENT ID: * {}",Thread.currentThread().getName(),item.getStudentID().toString());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            AlgorithmResponse algorithmResponse = this.runProjectedGradAlgorithm(item.getStudentID(), accessToken,summary.getBatchId());
            if(algorithmResponse.getException() != null) {
                ProcessError error = new ProcessError();
                error.setStudentID(item.getStudentID().toString());
                error.setReason(algorithmResponse.getException().getExceptionName());
                error.setDetail(algorithmResponse.getException().getExceptionDetails());
                summary.getErrors().add(error);
                summary.setProcessedCount(summary.getProcessedCount() - 1L);
                return null;
            }
            LOGGER.info("*** {} Partition  * Processed student[{}] * Student ID: {} in total {}",Thread.currentThread().getName(), summary.getProcessedCount(), item.getStudentID(), summary.getReadCount());
            return algorithmResponse.getGraduationStudentRecord();
        }catch(Exception e) {
            ProcessError error = new ProcessError();
            error.setStudentID(item.getStudentID().toString());
            error.setReason("GRAD-GRADUATION-API IS DOWN");
            error.setDetail("Graduation API is unavialble at this moment");
            summary.getErrors().add(error);
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info("*** {} Partition  - Processing Failed  * STUDENT ID: * {}",Thread.currentThread().getName(),item.getStudentID().toString());
            return null;
        }
    }

    public Integer getStudentByPenFromStudentAPI(List<LoadStudentData> loadStudentData, String accessToken) {
       AtomicReference<Integer> recordsAdded = new AtomicReference<>(0);
        loadStudentData.forEach(student -> {
            List<Student> stuDataList = this.getStudentsByPen(student.getPen(), accessToken);
            stuDataList.forEach(st-> {
                GraduationStudentRecord gradStu = new GraduationStudentRecord();
                gradStu.setProgram(student.getProgramCode());
                gradStu.setSchoolOfRecord(student.getSchool());
                gradStu.setStudentGrade(student.getStudentGrade());
                gradStu.setRecalculateGradStatus("Y");
                gradStu.setStudentStatus(student.getStudentStatus());
                gradStu.setStudentID(UUID.fromString(st.getStudentID()));
                this.saveGraduationStudentRecord(gradStu, accessToken);
                recordsAdded.getAndSet(recordsAdded.get() + 1);
            });
        });
        return recordsAdded.get();
    }

    public List<GraduationStudentRecord> getStudentData(List<UUID> studentIds, String accessToken) {
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        StudentList stuList = new StudentList();
        stuList.setStudentids(studentIds);
        return this.webClient.post()
                .uri(String.format(constants.getGradStudentApiStudentDataListUrl()))
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(stuList))
                .retrieve().bodyToMono(responseType).block();
    }

    public StudentCredentialDistribution processDistribution(StudentCredentialDistribution item, DistributionSummaryDTO summary) {
        LOGGER.info("*** {} Partition  - Processing  * STUDENT ID: * {}",Thread.currentThread().getName(),item.getStudentID().toString());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        String accessToken = summary.getAccessToken();

        StudentCredentialDistribution scObj = summary.getGlobalList().stream().filter(pr -> pr.getStudentID().compareTo(item.getStudentID()) == 0)
                .findAny()
                .orElse(null);
        if(scObj != null) {
            item.setSchoolOfRecord(scObj.getSchoolOfRecord());
        }else {
            GradSearchStudent stuRec =this.getStudentData(item.getStudentID().toString(),accessToken);
            if (stuRec != null) {
                item.setSchoolOfRecord(stuRec.getSchoolOfRecord());
            }
        }
        summary.getGlobalList().add(item);
        LOGGER.info("*** {} Partition  * Processed student[{}] * Student ID: {} in total {}",Thread.currentThread().getName(), summary.getProcessedCount(), item.getStudentID(), summary.getReadCount());
        return item;
    }

    public GradSearchStudent getStudentData(String studentID, String accessToken) {

        GradSearchStudent result = webClient.get()
                .uri(String.format(constants.getStudentInfo(),studentID))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(GradSearchStudent.class)
                .block();

        if(result != null)
            LOGGER.info("*** Fetched # of Graduation Record : {}",result.getStudentID());

        return result;
    }

    public void mergeAndUpload(Long batchId, String accessToken, Map<String, DistributionPrintRequest> mapDist) {

        DistributionResponse result = webClient.post()
                .uri(String.format(constants.getMergeAndUpload(),batchId))
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(mapDist))
                .retrieve()
                .bodyToMono(DistributionResponse.class)
                .block();

        LOGGER.info("Merge and Upload Success {}",result.getMergeProcessResponse());
    }
}
