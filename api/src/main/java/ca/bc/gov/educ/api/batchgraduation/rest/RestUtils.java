package ca.bc.gov.educ.api.batchgraduation.rest;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;
import io.github.resilience4j.retry.annotation.Retry;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class RestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestUtils.class);
    private static final String STUDENT_PROCESS = "*** {} Partition  - Processing  * STUDENT ID: * {}";
    private static final String STUDENT_PROCESSED = "*** {} Partition  * Processed student[{}] * Student ID: {} in total {}";
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
        LOGGER.debug("url = {}",constants.getTokenUrl());
        return this.webClient.post().uri(constants.getTokenUrl())
                .headers(h -> h.addAll(httpHeadersKC))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .bodyToMono(ResponseObj.class).block();
    }

    public List<Student> getStudentsByPen(String pen, String accessToken) {
        // No need to add a correlationID here.
        final ParameterizedTypeReference<List<Student>> responseType = new ParameterizedTypeReference<>() {
        };
        LOGGER.debug("url = {}",constants.getPenStudentApiByPenUrl());
        return this.webClient.get()
                .uri(String.format(constants.getPenStudentApiByPenUrl(), pen))
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    @Retry(name = "reggradrun")
    public AlgorithmResponse runGradAlgorithm(UUID studentID, String accessToken, String programCompleteDate,Long batchId) {
        UUID correlationID = UUID.randomUUID();
        if(programCompleteDate != null) {
            return this.webClient.get()
            		.uri(String.format(constants.getGraduationApiReportOnlyUrl(), studentID,batchId))
                    .headers(h -> {
                        h.setBearerAuth(accessToken);
                        h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                    })
                    .retrieve().bodyToMono(AlgorithmResponse.class).block();
        }
    	return this.webClient.get()
        		.uri(String.format(constants.getGraduationApiUrl(), studentID,batchId))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(AlgorithmResponse.class).block();
    }

    @Retry(name = "tvrrun")
    public AlgorithmResponse runProjectedGradAlgorithm(UUID studentID, String accessToken,Long batchId) {
        UUID correlationID = UUID.randomUUID();
        return this.webClient.get()
            .uri(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId))
            .headers(h -> {
                h.setBearerAuth(accessToken);
                h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
            })
            .retrieve().bodyToMono(AlgorithmResponse.class).block();

    }
    
    public List<GraduationStudentRecord> getStudentsForAlgorithm(String accessToken) {
        UUID correlationID = UUID.randomUUID();
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradStudentApiStudentForGradListUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<GraduationStudentRecord> getStudentsForProjectedAlgorithm(String accessToken) {
        UUID correlationID = UUID.randomUUID();
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradStudentApiStudentForProjectedGradListUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    // EDUC-GRAD-STUDENT-API ========================================

    public GraduationStudentRecord saveGraduationStudentRecord(GraduationStudentRecord graduationStudentRecord, String accessToken) {
        // No need to add a correlationID here.
        return this.webClient.post()
                .uri(String.format(constants.getGradStudentApiGradStatusUrl(),graduationStudentRecord.getStudentID()))
                .headers(h -> h.setBearerAuth(accessToken))
                .body(BodyInserters.fromValue(graduationStudentRecord))
                .retrieve().bodyToMono(GraduationStudentRecord.class).block();
    }

    public List<GraduationStudentRecord> getStudentsForSpecialGradRun(StudentSearchRequest req,String accessToken) {
        UUID correlationID = UUID.randomUUID();
        GraduationStudentRecordSearchResult res = this.webClient.post()
                .uri(constants.getGradStudentApiStudentForSpcGradListUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(req))
                .retrieve()
                .bodyToMono(GraduationStudentRecordSearchResult.class)
                .block();
        return res != null ?res.getGraduationStudentRecords():new ArrayList<>();
    }

    public List<GraduationStudentRecord> getStudentListByMinCode(String schoolOfRecord,String accessToken) {
        UUID correlationID = UUID.randomUUID();
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(String.format(constants.getGradStudentListSchoolReport(),schoolOfRecord))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve().bodyToMono(responseType).block();
    }

    public GraduationStudentRecord processStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
        LOGGER.info(STUDENT_PROCESS,Thread.currentThread().getName(),item.getStudentID());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            AlgorithmResponse algorithmResponse = this.runGradAlgorithm(item.getStudentID(), accessToken,item.getProgramCompletionDate(),summary.getBatchId());
            if(algorithmResponse.getException() != null) {
                summary.updateError(item.getStudentID(),algorithmResponse.getException().getExceptionName(),algorithmResponse.getException().getExceptionDetails());
                summary.setProcessedCount(summary.getProcessedCount() - 1L);
                return null;
            }
            LOGGER.info(STUDENT_PROCESSED,Thread.currentThread().getName(), summary.getProcessedCount(), item.getStudentID(), summary.getReadCount());
            summary.getSuccessfulStudentIDs().add(item.getStudentID());
            return algorithmResponse.getGraduationStudentRecord();
        }catch(Exception e) {
            summary.updateError(item.getStudentID(),"GRAD-GRADUATION-API IS DOWN","Graduation API is unavailable at this moment");
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info("*** {} Partition  - Processing Failed  * STUDENT ID: * {} Error Count : {}",Thread.currentThread().getName(),item.getStudentID(),summary.getErrors().size());
            return null;
        }
    }

    public GraduationStudentRecord processProjectedGradStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
        LOGGER.info(STUDENT_PROCESS,Thread.currentThread().getName(),item.getStudentID());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            AlgorithmResponse algorithmResponse = this.runProjectedGradAlgorithm(item.getStudentID(), accessToken,summary.getBatchId());
            if(algorithmResponse.getException() != null) {
                summary.updateError(item.getStudentID(),algorithmResponse.getException().getExceptionName(),algorithmResponse.getException().getExceptionDetails());
                summary.setProcessedCount(summary.getProcessedCount() - 1L);
                return null;
            }
            LOGGER.info(STUDENT_PROCESSED,Thread.currentThread().getName(), summary.getProcessedCount(), item.getStudentID(), summary.getReadCount());
            summary.getSuccessfulStudentIDs().add(item.getStudentID());
            summary.getGlobalList().add(item);
            return algorithmResponse.getGraduationStudentRecord();
        }catch(Exception e) {
            summary.updateError(item.getStudentID(),"GRAD-GRADUATION-API IS DOWN","Graduation API is unavailable at this moment");
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info("*** {} Partition  - Processing Failed  * STUDENT ID: * {} Error Count: {}",Thread.currentThread().getName(),item.getStudentID(),summary.getErrors().size());
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
        UUID correlationID = UUID.randomUUID();
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        StudentList stuList = new StudentList();
        stuList.setStudentids(studentIds);
        return this.webClient.post()
                .uri(constants.getGradStudentApiStudentDataListUrl())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(stuList))
                .retrieve().bodyToMono(responseType).block();
    }


    public SchoolReportDistribution processSchoolReportPosting(SchoolReportDistribution item, SchoolReportSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        summary.getGlobalList().add(item);
        LOGGER.info("Report Type Thread : {} Processed: {} Type: {} from {}",Thread.currentThread().getName(), summary.getProcessedCount(), item.getReportTypeCode(), summary.getReadCount());
        return item;
    }

    public StudentCredentialDistribution processDistribution(StudentCredentialDistribution item, DistributionSummaryDTO summary) {
        LOGGER.info(STUDENT_PROCESS,Thread.currentThread().getName(),item.getStudentID());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        String accessToken = summary.getAccessToken();

        StudentCredentialDistribution scObj = summary.getGlobalList().stream().filter(pr -> pr.getStudentID().compareTo(item.getStudentID()) == 0)
                .findAny()
                .orElse(null);
        if(scObj != null) {
            item.setSchoolOfRecord(scObj.getSchoolOfRecord());
        }else {
            GraduationStudentRecordDistribution stuRec =this.getStudentData(item.getStudentID().toString(),accessToken);
            if (stuRec != null) {
                item.setSchoolOfRecord(stuRec.getSchoolOfRecord());
                item.setProgram(stuRec.getProgram());
                item.setHonoursStanding(stuRec.getHonoursStanding());
                item.setSchoolOfRecord(stuRec.getSchoolOfRecord());
                item.setProgramCompletionDate(stuRec.getProgramCompletionDate());
                item.setStudentID(stuRec.getStudentID());
                item.setPen(stuRec.getPen());
                item.setLegalFirstName(stuRec.getLegalFirstName());
                item.setLegalMiddleNames(stuRec.getLegalMiddleNames());
                item.setLegalLastName(stuRec.getLegalLastName());
                item.setNonGradReasons(stuRec.getNonGradReasons());
                item.setStudentGrade(stuRec.getStudentGrade());
            }
        }
        summary.getGlobalList().add(item);
        LOGGER.info(STUDENT_PROCESSED,Thread.currentThread().getName(), summary.getProcessedCount(), item.getStudentID(), summary.getReadCount());
        return item;
    }

    public BlankCredentialDistribution processBlankDistribution(BlankCredentialDistribution item, BlankDistributionSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        String accessToken = summary.getAccessToken();
        String credentialType = summary.getCredentialType();
        if (credentialType != null && credentialType.equalsIgnoreCase("OC")){
            GradCertificateTypes certType = this.getCertTypes(item.getCredentialTypeCode(), accessToken);
            if (certType != null)
                item.setPaperType(certType.getPaperType());
            else
                item.setPaperType("YED4");
        }else {
            item.setPaperType("YED4");
        }
        summary.getGlobalList().add(item);
        return item;
    }

    public GradCertificateTypes getCertTypes(String certType,String accessToken) {
        UUID correlationID = UUID.randomUUID();
        GradCertificateTypes result = webClient.get()
                .uri(String.format(constants.getCertificateTypes(),certType))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve()
                .bodyToMono(GradCertificateTypes.class)
                .block();
        if(result != null)
            LOGGER.info("*** Fetched # of Cert type Record : {}",result.getCode());

        return result;
    }
    public GraduationStudentRecordDistribution getStudentData(String studentID, String accessToken) {
        UUID correlationID = UUID.randomUUID();
        GraduationStudentRecordDistribution result = webClient.get()
                .uri(String.format(constants.getStudentInfo(),studentID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .retrieve()
                .bodyToMono(GraduationStudentRecordDistribution.class)
                .block();

        if(result != null)
            LOGGER.info("*** Fetched # of Graduation Record : {}",result.getStudentID());

        return result;
    }

    public DistributionResponse readAndPostSchoolReports(Long batchId,String accessToken, Map<String, DistributionPrintRequest> mapDist) {
        UUID correlationID = UUID.randomUUID();
        DistributionResponse result = webClient.post()
                .uri(String.format(constants.getReadAndPost(),batchId))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(mapDist))
                .retrieve()
                .bodyToMono(DistributionResponse.class)
                .block();

        if(result != null)
            LOGGER.info("Read and Post Success {}",result.getMergeProcessResponse());
        return  result;
    }

    public void createAndStoreSchoolReports(String accessToken, Map<String, SchoolReportRequest> mapDist) {
        UUID correlationID = UUID.randomUUID();
        Integer result = webClient.post()
                .uri(constants.getCreateAndStore())
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(mapDist))
                .retrieve()
                .bodyToMono(Integer.class)
                .block();

        if(result != null && result != 0)
            LOGGER.info("Create and Store School Report Success {}",result);
    }

    public DistributionResponse mergeAndUpload(Long batchId, String accessToken, Map<String, DistributionPrintRequest> mapDist,String activityCode,String localDownload) {
        UUID correlationID = UUID.randomUUID();
        String url;
        if(activityCode.equalsIgnoreCase("YEARENDDIST")) {
            url= String.format(constants.getMergeAndUploadYearly(),batchId,activityCode);
        }else {
            url = String.format(constants.getMergeAndUpload(),batchId,activityCode,localDownload);
        }
        DistributionResponse result = webClient.post()
                .uri(url)
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(mapDist))
                .retrieve()
                .bodyToMono(DistributionResponse.class)
                .block();

        if(result != null)
            LOGGER.info("Merge and Upload Success {}",result.getMergeProcessResponse());
        return  result;
    }


    public void createBlankCredentialsAndUpload(Long batchId, String accessToken, Map<String, DistributionPrintRequest> mapDist,String localDownload) {
        UUID correlationID = UUID.randomUUID();
        DistributionResponse result = webClient.post()
                .uri(String.format(constants.getCreateBlanksAndUpload(),batchId,localDownload))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(mapDist))
                .retrieve()
                .bodyToMono(DistributionResponse.class)
                .block();
        if(result != null)
            LOGGER.info("Create and Upload Success {}",result.getMergeProcessResponse());
    }

    public DistributionResponse createReprintAndUpload(Long batchId, String accessToken, Map<String, DistributionPrintRequest> mapDist, String activityCode,String localDownload) {
        UUID correlationID = UUID.randomUUID();
        DistributionResponse result = webClient.post()
                .uri(String.format(constants.getReprintAndUpload(),batchId,activityCode,localDownload))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(mapDist))
                .retrieve()
                .bodyToMono(DistributionResponse.class)
                .block();
        if(result != null)
            LOGGER.info("Merge and Upload Success {}",result.getMergeProcessResponse());
        return  result;
    }

    public void updateStudentCredentialRecord(UUID studentID, String credentialTypeCode, String paperType,String documentStatusCode,String accessToken) {
        UUID correlationID = UUID.randomUUID();
        webClient.get().uri(String.format(constants.getUpdateStudentCredential(),studentID,credentialTypeCode,paperType,documentStatusCode))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                }).retrieve().bodyToMono(boolean.class).block();
    }

    public void updateSchoolReportRecord(String schoolOfRecord, String reportTypeCode,String accessToken) {
        UUID correlationID = UUID.randomUUID();
        webClient.get().uri(String.format(constants.getUpdateSchoolReport(),schoolOfRecord,reportTypeCode))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                }).retrieve().bodyToMono(boolean.class).block();
    }


    public List<StudentCredentialDistribution> getStudentsForUserReqDisRun(String credentialType, StudentSearchRequest req, String accessToken) {
        UUID correlationID = UUID.randomUUID();
        final ParameterizedTypeReference<List<StudentCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.post()
                .uri(String.format(constants.getStudentDataForUserReqDisRun(),credentialType))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                })
                .body(BodyInserters.fromValue(req))
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    public void updateStudentGradRecord(UUID studentID, Long batchId,String activityCode, String accessToken) {
        UUID correlationID = UUID.randomUUID();
        webClient.post().uri(String.format(constants.getUpdateStudentRecord(),studentID,batchId,activityCode)).headers(h -> {
            h.setBearerAuth(accessToken);
            h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
        }).retrieve().bodyToMono(GraduationStudentRecord.class).block();
    }

    public GraduationStudentRecord getGradStatus(UUID studentID, String accessToken) {
        UUID correlationID = UUID.randomUUID();
        return webClient.get().uri(String.format(constants.getReadGradStudentRecord(),studentID))
            .headers(h -> {
                h.setBearerAuth(accessToken);
                h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
        }).retrieve().bodyToMono(GraduationStudentRecord.class).block();
    }

    public GraduationStudentRecord getGradStatusForBatch(UUID studentID, String accessToken) {
        UUID correlationID = UUID.randomUUID();
        return webClient.get().uri(String.format(constants.getReadGradStudentRecordBatch(),studentID))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString());
                }).retrieve().bodyToMono(GraduationStudentRecord.class).block();
    }

}
