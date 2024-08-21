package ca.bc.gov.educ.api.batchgraduation.rest;

import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiUtils;
import ca.bc.gov.educ.api.batchgraduation.util.JsonTransformer;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
public class RestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestUtils.class);
    private static final String STUDENT_READ = "R:{}";
    private static final String STUDENT_PROCESS = "P:{}";
    private static final String STUDENT_PROCESSED = "D:{} {} of {} batch {}";
    private static final String TRAX_API_IS_DOWN = "Trax API is not available {}";
    private static final String URL_FORMAT_STR = "url = {}";
    private static final String GRADUATION_API_IS_DOWN = "GRAD-GRADUATION-API IS DOWN";
    private static final String GRADUATION_API_DOWN_MSG = "Graduation API is unavailable at this moment";
    private static final String FAILED_STUDENT_ERROR_MSG = "Failed STU-ID:{} Errors:{}";
    private static final String MERGE_MSG="Merge and Upload Success {}";
    private static final String YEARENDDIST = "YEARENDDIST";
    private static final String SUPPDIST = "SUPPDIST";
    private static final String NONGRADYERUN = "NONGRADYERUN";
    private static final String ERROR_MESSAGE1 = "Service failed to process after max retries.";
    private static final String ERROR_MESSAGE2 = "5xx error.";

    final EducGradBatchGraduationApiConstants constants;
    final ResponseObjCache responseObjCache;

    final WebClient webClient;

    final JsonTransformer jsonTransformer;

    final GraduationReportService graduationReportService;

    @Autowired
    public RestUtils(final GraduationReportService graduationReportService, final EducGradBatchGraduationApiConstants constants, final WebClient webClient, final ResponseObjCache objCache, final JsonTransformer jsonTransformer) {
        this.graduationReportService = graduationReportService;
        this.constants = constants;
        this.webClient = webClient;
        this.responseObjCache = objCache;
        this.jsonTransformer = jsonTransformer;
    }

    public ResponseObj getTokenResponseObject() {
        if(responseObjCache.isExpired()){
            responseObjCache.setResponseObj(getResponseObj());
        }
        return responseObjCache.getResponseObj();
    }

    public String fetchAccessToken() {
        return this.getTokenResponseObject().getAccess_token();
    }

    /**
     * Generic GET call out to services. Uses blocking webclient and will throw
     * runtime exceptions. Will attempt retries if 5xx errors are encountered.
     * You can catch Exception in calling method.
     * @param url the url you are calling
     * @param clazz the return type you are expecting
     * @param accessToken access token
     * @return return type
     * @param <T> expected return type
     */
    public <T> T get(String url, Class<T> clazz, String accessToken) {
        T obj;
        try {
            obj = this.webClient
                    .get()
                    .uri(url)
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()); })
                    .retrieve()
                    // if 5xx errors, throw Service error
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    // only does retry if initial error was 5xx as service may be temporarily down
                    // 4xx errors will always happen if 404, 401, 403 etc., so does not retry
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            // catches IOExceptions and the like
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
        return obj;
    }

    /**
     * Generic POST call out to services. Uses blocking webclient and will throw
     * runtime exceptions. Will attempt retries if 5xx errors are encountered.
     * You can catch Exception in calling method.
     * @param url the url you are calling
     * @param body the body you are requesting
     * @param clazz the return type you are expecting
     * @param accessToken access token
     * @return return type
     * @param <T> expected return type
     */
    public <T> T post(String url, Object body, Class<T> clazz, String accessToken) {
        T obj;
        try {
            obj = webClient.post()
                    .uri(url)
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()); })
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
        return obj;
    }

    /**
     * Generic PUT call out to services. Uses blocking webclient and will throw
     * runtime exceptions. Will attempt retries if 5xx errors are encountered.
     * You can catch Exception in calling method.
     * @param url the url you are calling
     * @param body the body you are requesting
     * @param clazz the return type you are expecting
     * @param accessToken access token
     * @return return type
     * @param <T> expected return type
     */
    public <T> T put(String url, Object body, Class<T> clazz, String accessToken) {
        T obj;
        try {
            obj = this.webClient.put()
                    .uri(url)
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()); })
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError,
                            clientResponse -> Mono.error(new ServiceException(getErrorMessage(url, ERROR_MESSAGE1), clientResponse.statusCode().value())))
                    .bodyToMono(clazz)
                    .retryWhen(reactor.util.retry.Retry.backoff(constants.getDefaultRetryMaxAttempts(), Duration.ofSeconds(constants.getDefaultRetryWaitDurationSeconds()))
                            .filter(ServiceException.class::isInstance)
                            .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                throw new ServiceException(getErrorMessage(url, ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                            }))
                    .block();
        } catch (Exception e) {
            throw new ServiceException(getErrorMessage(url, e.getLocalizedMessage()), HttpStatus.SERVICE_UNAVAILABLE.value(), e);
        }
        return obj;
    }


    private String getErrorMessage(String url, String errorMessage) {
        return "Service failed to process at url: " + url + " due to: " + errorMessage;
    }

    private ResponseObj getResponseObj() {
        LOGGER.info("Fetch token");
        HttpHeaders httpHeadersKC = EducGradBatchGraduationApiUtils.getHeaders(
                constants.getUserName(), constants.getPassword());
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        return this.webClient.post().uri(constants.getTokenUrl())
                .headers(h -> h.addAll(httpHeadersKC))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError,
                        clientResponse -> Mono.error(new ServiceException(getErrorMessage(constants.getTokenUrl(), ERROR_MESSAGE1), clientResponse.statusCode().value())))
                .bodyToMono(ResponseObj.class)
                .retryWhen(reactor.util.retry.Retry.backoff(constants.getTokenRetryMaxAttempts(), Duration.ofSeconds(constants.getTokenRetryWaitDurationSeconds()))
                        .filter(ServiceException.class::isInstance)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new ServiceException(getErrorMessage(constants.getTokenUrl(), ERROR_MESSAGE2), HttpStatus.SERVICE_UNAVAILABLE.value());
                        }))
                .block();
    }

    @SuppressWarnings("rawtypes")
    public List<Student> getStudentsByPen(String pen, String accessToken) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        String url = String.format(constants.getPenStudentApiByPenUrl(), pen);
        LOGGER.debug(URL_FORMAT_STR, url);
        List response = this.get(url, List.class, accessToken);
        if (response != null && !response.isEmpty()) {
            return jsonTransformer.convertValue(response, new TypeReference<>() {});
        }
        return new ArrayList<>();
    }

    public AlgorithmResponse runGradAlgorithm(UUID studentID, String accessToken, String gradProgram, String programCompleteDate,Long batchId) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        String url = isReportOnly(studentID, gradProgram, programCompleteDate, accessToken)?
            String.format(constants.getGraduationApiReportOnlyUrl(), studentID, batchId) : String.format(constants.getGraduationApiUrl(), studentID, batchId);
        return this.get(url, AlgorithmResponse.class, accessToken);
    }

    public AlgorithmResponse runProjectedGradAlgorithm(UUID studentID, String accessToken,Long batchId) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        return this.get(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId), AlgorithmResponse.class, accessToken);
    }

    public BatchGraduationStudentRecord runGetStudentForBatchInput(UUID studentID, String accessToken) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        return this.get(String.format(constants.getGradStudentApiGradStatusForBatchUrl(), studentID), BatchGraduationStudentRecord.class, accessToken);
    }

    public BatchGraduationStudentRecord getStudentForBatchInput(UUID studentID, AlgorithmSummaryDTO summary) {
        LOGGER.debug(STUDENT_READ,studentID);
        try {
            return this.runGetStudentForBatchInput(studentID, summary.getAccessToken());
        } catch(Exception e) {
            summary.updateError(studentID,"GRAD-STUDENT-API IS DOWN","GRAD Student API is unavailable at this moment");
            LOGGER.info("GET Failed STU-ID:{} Errors:{}",studentID,summary.getErrors().size());
            return null;
        }
    }

    public List<StudentCredentialDistribution> fetchDistributionRequiredDataStudentsNonGradYearly() {
        return graduationReportService.getStudentsNonGradForYearlyDistribution(getTokenResponseObject().getAccess_token());
    }

    public List<StudentCredentialDistribution> fetchDistributionRequiredDataStudentsNonGradYearly(String mincode) {
        return graduationReportService.getStudentsNonGradForYearlyDistribution(mincode, getTokenResponseObject().getAccess_token());
    }

    public List<StudentCredentialDistribution> fetchDistributionRequiredDataStudentsYearly() {
        return graduationReportService.getStudentsForYearlyDistribution(getTokenResponseObject().getAccess_token());
    }


    public Integer runRegenerateStudentCertificate(String pen) {
        UUID correlationID = UUID.randomUUID();
        String accessToken = getAccessToken();
        return this.webClient.get()
                .uri(String.format(constants.getStudentCertificateRegeneration(), pen),
                        uri -> uri.queryParam("isOverwrite", "N").build())
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .retrieve().bodyToMono(Integer.class).block();
    }

    public List<UUID> getStudentsForAlgorithm() {
        UUID correlationID = UUID.randomUUID();
        String accessToken = getAccessToken();
        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradStudentApiStudentForGradListUrl())
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .retrieve().bodyToMono(responseType).block();
    }

    public List<UUID> getStudentsForProjectedAlgorithm() {
        UUID correlationID = UUID.randomUUID();
        String accessToken = getAccessToken();
        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.get()
                .uri(constants.getGradStudentApiStudentForProjectedGradListUrl())
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
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

    public List<UUID> getStudentsForSpecialGradRun(StudentSearchRequest req) {
        UUID correlationID = UUID.randomUUID();
        String accessToken = getAccessToken();
        GraduationStudentRecordSearchResult res = this.webClient.post()
                .uri(constants.getGradStudentApiStudentForSpcGradListUrl())
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(req))
                .retrieve()
                .bodyToMono(GraduationStudentRecordSearchResult.class)
                .block();
        return res != null ?res.getStudentIDs() : new ArrayList<>();
    }

    public GraduationStudentRecord processStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
        LOGGER.debug(STUDENT_PROCESS,item.getStudentID());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            AlgorithmResponse algorithmResponse = this.runGradAlgorithm(item.getStudentID(), accessToken,
                    item.getProgram(), item.getProgramCompletionDate(), summary.getBatchId());
            return processGraduationStudentRecord(item, summary, algorithmResponse);
        }catch(Exception e) {
            summary.updateError(item.getStudentID(),GRADUATION_API_IS_DOWN,GRADUATION_API_DOWN_MSG);
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info(FAILED_STUDENT_ERROR_MSG,item.getStudentID(),summary.getErrors().size());
            return null;
        }
    }

    private GraduationStudentRecord processGraduationStudentRecord(GraduationStudentRecord item, AlgorithmSummaryDTO summary, AlgorithmResponse algorithmResponse) {
        if(algorithmResponse.getException() != null) {
            summary.updateError(item.getStudentID(),algorithmResponse.getException().getExceptionName(),algorithmResponse.getException().getExceptionDetails());
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            return null;
        }
        LOGGER.info(STUDENT_PROCESSED, item.getStudentID(), summary.getProcessedCount(), summary.getReadCount(), summary.getBatchId());
        return algorithmResponse.getGraduationStudentRecord();
    }

    public boolean isReportOnly(UUID studentID, String gradProgram, String programCompletionDate, String accessToken) {
        boolean isFMR = false;
        if ("SCCP".equalsIgnoreCase(gradProgram)) {
            if (programCompletionDate != null) {
                Date pCD = EducGradBatchGraduationApiUtils.parsingTraxDate(programCompletionDate);
                int diff = EducGradBatchGraduationApiUtils.getDifferenceInDays(EducGradBatchGraduationApiUtils.getProgramCompletionDate(pCD), EducGradBatchGraduationApiUtils.getCurrentDate());
                if (diff >= 0) {
                    isFMR = checkSccpCertificateExists(studentID, accessToken);
                } else {
                    isFMR = false;
                }
            }
        } else {
            isFMR = programCompletionDate != null;
        }
        return isFMR;
    }

    public GraduationStudentRecord processProjectedGradStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
        LOGGER.info(STUDENT_PROCESS,item.getStudentID());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            AlgorithmResponse algorithmResponse = this.runProjectedGradAlgorithm(item.getStudentID(), accessToken,summary.getBatchId());
            return processGraduationStudentRecord(item, summary, algorithmResponse);
        } catch(Exception e) {
            summary.updateError(item.getStudentID(),GRADUATION_API_IS_DOWN,GRADUATION_API_DOWN_MSG);
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info(FAILED_STUDENT_ERROR_MSG,item.getStudentID(),summary.getErrors().size());
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
                saveGraduationStudentRecord(gradStu, accessToken);
                recordsAdded.getAndSet(recordsAdded.get() + 1);
            });
        });
        return recordsAdded.get();
    }

    public List<GraduationStudentRecord> getStudentData(List<UUID> studentIds) {
        UUID correlationID = UUID.randomUUID();
        final ParameterizedTypeReference<List<GraduationStudentRecord>> responseType = new ParameterizedTypeReference<>() {
        };
        StudentList stuList = new StudentList();
        stuList.setStudentids(studentIds);
        String accessToken = getAccessToken();
        return this.webClient.post()
                .uri(constants.getGradStudentApiStudentDataListUrl())
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(stuList))
                .retrieve().bodyToMono(responseType).block();
    }

    public StudentCredentialDistribution processDistribution(StudentCredentialDistribution item, DistributionSummaryDTO summary, boolean useSchoolAtGrad) {
        LOGGER.info(STUDENT_PROCESS,item.getStudentID());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        StudentCredentialDistribution scObj = summary.getGlobalList().stream().filter(pr -> pr.getStudentID().compareTo(item.getStudentID()) == 0)
                .findAny()
                .orElse(null);
        if(scObj != null) {
            if(useSchoolAtGrad) {
                item.setSchoolOfRecord(StringUtils.isBlank(scObj.getSchoolAtGrad()) ? scObj.getSchoolOfRecord() : scObj.getSchoolAtGrad());
            } else {
                item.setSchoolOfRecord(scObj.getSchoolOfRecord());
            }
            item.setPen(scObj.getPen());
            item.setLegalLastName(scObj.getLegalLastName());
            item.setLegalFirstName(scObj.getLegalFirstName());
            item.setLegalMiddleNames(scObj.getLegalMiddleNames());
        } else {
            GraduationStudentRecordDistribution stuRec = getStudentData(item.getStudentID().toString());
            if (stuRec != null) {
                item.setProgram(stuRec.getProgram());
                item.setHonoursStanding(stuRec.getHonoursStanding());
                if(useSchoolAtGrad) {
                    item.setSchoolOfRecord(StringUtils.isBlank(stuRec.getSchoolAtGrad()) ? stuRec.getSchoolOfRecord() : stuRec.getSchoolAtGrad());
                } else {
                    item.setSchoolOfRecord(stuRec.getSchoolOfRecord());
                }
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
        LOGGER.info(STUDENT_PROCESSED, item.getStudentID(), summary.getProcessedCount(), summary.getReadCount(), summary.getBatchId());
        return item;
    }

    public PsiCredentialDistribution processPsiDistribution(PsiCredentialDistribution item, PsiDistributionSummaryDTO summary) {
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        String accessToken = summary.getAccessToken();
        try {
            List<Student>  stuDataList = this.getStudentsByPen(item.getPen(), accessToken);
            if(!stuDataList.isEmpty()) {
                item.setStudentID(UUID.fromString(stuDataList.get(0).getStudentID()));
            }
            summary.getGlobalList().add(item);
        } catch (Exception e) {
            LOGGER.error("Error processing student with id {} due to {}", item.getStudentID(), e.getLocalizedMessage());
            summary.getErrors().add(
                    new ProcessError(item.getStudentID().toString(), e.getLocalizedMessage(), e.getMessage())
            );
        }
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
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .retrieve()
                .bodyToMono(GradCertificateTypes.class)
                .block();
        if(result != null)
            LOGGER.info("Fetched {} Cert type Records : ",result.getCode());

        return result;
    }

    public GraduationStudentRecord getStudentDataForBatch(String studentID, String accessToken) {
        UUID correlationID = UUID.randomUUID();
        GraduationStudentRecord result = webClient.get()
                .uri(String.format(constants.getStudentInfo(),studentID))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .retrieve()
                .bodyToMono(GraduationStudentRecord.class)
                .block();

        if(result != null)
            LOGGER.info("Fetched {} Graduation Records",result.getStudentID());

        return result;
    }

    public GraduationStudentRecordDistribution getStudentData(String studentID) {
        String accessToken = getAccessToken();
        String url = String.format(constants.getStudentInfo(),studentID);
        GraduationStudentRecordDistribution result = this.get(url, GraduationStudentRecordDistribution.class, accessToken);
        if(result != null)
            LOGGER.info("Fetched {} Graduation Records",result.getStudentID());
        return result;
    }

    public Integer createAndStoreSchoolReports(List<String> uniqueSchools, String type) {
        UUID correlationID = UUID.randomUUID();
        Integer result = 0;
        if(uniqueSchools == null || uniqueSchools.isEmpty()) {
            LOGGER.info("{} Schools selected for School Reports", result);
            return result;
        }
        int pageSize = 10;
        int pageNum = uniqueSchools.size() / pageSize + 1;
        for (int i = 0; i < pageNum; i++) {
            int startIndex = i * pageSize;
            int endIndex = Math.min(startIndex + pageSize, uniqueSchools.size());
            List<String> mincodes = uniqueSchools.subList(startIndex, endIndex);
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating School Reports for schools {}", mincodes.stream().collect(Collectors.joining(",", "{", "}")));
            }
            result += webClient.post()
                    .uri(String.format(constants.getCreateAndStoreSchoolReports(),type))
                    .headers(h -> { h.setBearerAuth(getAccessToken()); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(mincodes))
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .block();
        }
        LOGGER.info("Created and Stored {} School Reports", result);
        return result;
    }

    public Integer processStudentReports(List<UUID> uuidList, String studentReportType) {
        UUID correlationID = UUID.randomUUID();
        Integer result = webClient.post()
                .uri(String.format(constants.getUpdateStudentReport(), studentReportType))
                .headers(h -> { h.setBearerAuth(getAccessToken()); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(uuidList))
                .retrieve()
                .bodyToMono(Integer.class)
                .block();
        LOGGER.info("{} Student {} Reports", result, studentReportType);
        return result;
    }

    //Grad2-1931 sending transmissionType with the webclient.
    public DistributionResponse mergePsiAndUpload(Long batchId, String accessToken, DistributionRequest distributionRequest,String localDownload, String transmissionType) {
        UUID correlationID = UUID.randomUUID();
        DistributionResponse result = webClient.post()
                .uri(String.format(constants.getMergePsiAndUpload(),batchId,localDownload,transmissionType))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(distributionRequest))
                .retrieve()
                .bodyToMono(DistributionResponse.class)
                .block();

        if(result != null)
            LOGGER.info(MERGE_MSG,result.getMergeProcessResponse());
        return  new DistributionResponse();
    }

    public DistributionResponse mergeAndUpload(Long batchId, DistributionRequest distributionRequest, String activityCode, String localDownload) {
        String distributionUrl;
        distributionRequest.setActivityCode(activityCode);
        String accessToken = getAccessToken();
        if(YEARENDDIST.equalsIgnoreCase(activityCode)) {
            distributionUrl = String.format(constants.getMergeAndUploadYearly(),batchId,activityCode);
        } else if(NONGRADYERUN.equalsIgnoreCase(activityCode)) {
            distributionUrl = String.format(constants.getMergeAndUploadYearly(),batchId,activityCode);
        } else if(SUPPDIST.equalsIgnoreCase(activityCode)) {
            distributionUrl = String.format(constants.getMergeAndUploadSupplemental(),batchId,activityCode);
        } else {
            distributionUrl = String.format(constants.getMergeAndUpload(),batchId,activityCode,localDownload);
        }
        LOGGER.debug("****** Call distribution API {} to process the merge request for {} *******", distributionUrl, batchId);
        return this.post(distributionUrl, distributionRequest, DistributionResponse.class, accessToken);
    }

    public Boolean executePostDistribution(DistributionResponse distributionResponse) {
        UUID correlationID = UUID.randomUUID();
        return webClient.post()
                .uri(constants.getPostingDistribution())
                .headers(h -> { h.setBearerAuth(getAccessToken()); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(distributionResponse))
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();
    }

    public void createBlankCredentialsAndUpload(Long batchId, String accessToken, DistributionRequest distributionRequest, String localDownload) {
        UUID correlationID = UUID.randomUUID();
        DistributionResponse result = webClient.post()
                .uri(String.format(constants.getCreateBlanksAndUpload(),batchId,localDownload))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(distributionRequest))
                .retrieve()
                .bodyToMono(DistributionResponse.class)
                .block();
        if(result != null)
            LOGGER.info("Create and Upload Success {}",result.getMergeProcessResponse());
    }

    public DistributionResponse createReprintAndUpload(Long batchId, String accessToken, DistributionRequest distributionRequest, String activityCode,String localDownload) {
        UUID correlationID = UUID.randomUUID();
        distributionRequest.setActivityCode(activityCode);
        DistributionResponse result = webClient.post()
                .uri(String.format(constants.getReprintAndUpload(),batchId,activityCode,localDownload))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(distributionRequest))
                .retrieve()
                .bodyToMono(DistributionResponse.class)
                .block();
        if(result != null)
            LOGGER.info(MERGE_MSG,result.getMergeProcessResponse());
        return  result;
    }

    public void updateStudentCredentialRecord(UUID studentID, String credentialTypeCode, String paperType,String documentStatusCode,String activityCode,String accessToken) {
        String url = String.format(constants.getUpdateStudentCredential(),studentID,
                credentialTypeCode != null? credentialTypeCode : "",paperType,documentStatusCode,activityCode);
        this.get(url, boolean.class, accessToken);
    }

    public void updateSchoolReportRecord(String schoolOfRecord, String reportTypeCode, String accessToken) {
        UUID correlationID = UUID.randomUUID();
        webClient.get().uri(String.format(constants.getUpdateSchoolReport(),schoolOfRecord,reportTypeCode))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .retrieve().bodyToMono(boolean.class).block();
    }

    public void deleteSchoolReportRecord(String schoolOfRecord, String reportTypeCode) {
        UUID correlationID = UUID.randomUUID();
        String accessToken = getAccessToken();
        webClient.delete().uri(String.format(constants.getUpdateSchoolReport(),schoolOfRecord,reportTypeCode))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .retrieve().bodyToMono(boolean.class).block();
    }

    public List<StudentCredentialDistribution> getStudentsForUserReqDisRun(String credentialType, StudentSearchRequest req) {
        UUID correlationID = UUID.randomUUID();
        String accessToken = getAccessToken();
        final ParameterizedTypeReference<List<StudentCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.post()
                .uri(String.format(constants.getStudentDataForUserReqDisRun(),credentialType))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(req))
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    public List<StudentCredentialDistribution> getStudentsForUserReqDisRunWithNullDistributionDate(String credentialType, StudentSearchRequest req) {
        UUID correlationID = UUID.randomUUID();
        String accessToken = getAccessToken();
        final ParameterizedTypeReference<List<StudentCredentialDistribution>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.post()
                .uri(String.format(constants.getStudentDataForUserReqDisRunWithNullDistributionDate(),credentialType))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(req))
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }

    public void updateStudentGradRecord(UUID studentID, Long batchId,String activityCode) {
        //Grad2-1931 not updating the school record if student id does not exist.
        try {
            if (studentID != null) {
                String accessToken = getAccessToken();
                String url = String.format(constants.getUpdateStudentRecord(), studentID, batchId, activityCode);
                this.post(url, "{}", GraduationStudentRecord.class, accessToken);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to update student record {}", studentID);
        }
    }

    public void updateStudentGradRecordHistory(List<UUID> studentIDs, Long batchId, String accessToken, String userName, String activityCode) {
         try {
            if (batchId != null) {
                String url = String.format(constants.getUpdateStudentRecordHistory(), batchId, userName, activityCode);
                this.put(url,studentIDs, GraduationStudentRecord.class, accessToken);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to update student record history");
        }
    }

    public void updateStudentGradRecordHistory(List<UUID> studentIDs, Long batchId, String userName, String activityCode) {
        updateStudentGradRecordHistory(studentIDs, batchId, getAccessToken(), userName, activityCode);
    }

    public String updateStudentFlagReadyForBatch(List<UUID> studentIds, String batchJobType, String accessToken) {
        UUID correlationID = UUID.randomUUID();
        final ParameterizedTypeReference<String> responseType = new ParameterizedTypeReference<>() {
        };
        StudentList stuList = new StudentList();
        stuList.setStudentids(studentIds);
        return this.webClient.post()
                .uri(String.format(constants.getUpdateStudentFlagReadyForBatchByStudentIDs(), batchJobType))
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(stuList))
                .retrieve().bodyToMono(responseType).block();
    }

    public Boolean checkSccpCertificateExists (UUID studentID, String accessToken) {
        return this.webClient.get()
                .uri(constants.getCheckSccpCertificateExists(),
                        uri -> uri.queryParam("studentID", studentID).build())
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID()); })
                .retrieve().bodyToMono(Boolean.class).block();
    }

    public List<String> getEDWSnapshotSchools(Integer gradYear) {
        String accessToken = getAccessToken();
        final ParameterizedTypeReference<List<String>> responseType = new ParameterizedTypeReference<>() {
        };
        String url = String.format(constants.getEdwSnapshotSchoolsUrl(), gradYear);
        LOGGER.debug(URL_FORMAT_STR,url);
        return this.webClient.get()
                .uri(url)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    public List<SnapshotResponse> getEDWSnapshotStudents(Integer gradYear, String mincode) {
        String accessToken = getAccessToken();
        final ParameterizedTypeReference<List<SnapshotResponse>> responseType = new ParameterizedTypeReference<>() {
        };
        String url = String.format(constants.getEdwSnapshotStudentsByMincodeUrl(), gradYear, mincode);
        LOGGER.debug(URL_FORMAT_STR,url);
        return this.webClient.get()
                .uri(url)
                .headers(h -> h.setBearerAuth(accessToken))
                .retrieve().bodyToMono(responseType).block();
    }

    public String getAccessToken() {
        return this.fetchAccessToken();
    }


    public CommonSchool getCommonSchool(String schoolOfRecord) {
        return get(String.format(constants.getCommonSchoolByMincode(), schoolOfRecord), CommonSchool.class, getAccessToken());
    }

    public List<District> getDistrictBySchoolCategoryCode(String schoolCategoryCode) {
        final ParameterizedTypeReference<List<District>> responseType = new ParameterizedTypeReference<>() {
        };
        try {
            String url = String.format(constants.getTraxDistrictBySchoolCategory(), schoolCategoryCode);
            return webClient.get().uri(url)
                    .headers(h -> {
                        h.setBearerAuth(getAccessToken());
                        h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                    })
                    .retrieve().bodyToMono(responseType)
                    .block();
        } catch (Exception e) {
            LOGGER.error(TRAX_API_IS_DOWN, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    public List<School> getSchoolBySchoolCategoryCode(String schoolCategoryCode) {
        final ParameterizedTypeReference<List<School>> responseType = new ParameterizedTypeReference<>() {
        };
        try {
            String url = String.format(constants.getTraxSchoolBySchoolCategory(), schoolCategoryCode);
            return webClient.get().uri(url)
                    .headers(h -> {
                        h.setBearerAuth(getAccessToken());
                        h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                    })
                    .retrieve().bodyToMono(responseType)
                    .block();
        } catch (Exception e) {
            LOGGER.error(TRAX_API_IS_DOWN, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    public List<School> getSchoolByDistrictCode(String district) {
        final ParameterizedTypeReference<List<School>> responseType = new ParameterizedTypeReference<>() {
        };
        try {
            String url = String.format(constants.getTraxSchoolByDistrict(), district);
            return webClient.get().uri(url)
                    .headers(h -> {
                        h.setBearerAuth(getAccessToken());
                        h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, ThreadLocalStateUtil.getCorrelationID());
                    })
                    .retrieve().bodyToMono(responseType)
                    .block();
        } catch (Exception e) {
            LOGGER.error(TRAX_API_IS_DOWN, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    public TraxSchool getTraxSchool(String mincode) {
        return get(String.format(constants.getTraxSchoolByMincode(), mincode), TraxSchool.class, getAccessToken());
    }

    public List<UUID> getDeceasedStudentIDs(List<UUID> studentIDs) {
        UUID correlationID = UUID.randomUUID();
        String accessToken = getAccessToken();
        final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
        };
        return this.webClient.post()
                .uri(constants.getDeceasedStudentIDList())
                .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                .body(BodyInserters.fromValue(studentIDs))
                .retrieve().bodyToMono(responseType).block();
    }

    public UUID getStudentIDByPen(String pen, String accessToken) {
        try {
            List<Student>  stuDataList = this.getStudentsByPen(pen, accessToken);
            if(!stuDataList.isEmpty()) {
                return UUID.fromString(stuDataList.get(0).getStudentID());
            }
        } catch (Exception e) {
            LOGGER.error("Error processing student with pen# {} due to {}", pen, e.getLocalizedMessage());
        }
        return null;
    }

    public EdwGraduationSnapshot processSnapshot(EdwGraduationSnapshot item, EdwSnapshotSummaryDTO summary) {
        UUID correlationID = UUID.randomUUID();
        LOGGER.debug(STUDENT_PROCESS,item.getPen());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            String accessToken = summary.getAccessToken();
            return this.webClient.post()
                    .uri(constants.getSnapshotGraduationStatusForEdwUrl())
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(item))
                    .retrieve().bodyToMono(EdwGraduationSnapshot.class).block();
        } catch(Exception e) {
            summary.updateError(item.getPen(),item.getSchoolOfRecord(),GRADUATION_API_IS_DOWN,GRADUATION_API_DOWN_MSG);
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info("Failed STU-PEN:{} Errors:{}",item.getPen(),summary.getErrors().size());
            return null;
        }
    }

    public Long getTotalReportsForProcessing(List<String> finalSchoolDistricts, String reportType, DistributionSummaryDTO summaryDTO) {
        Long schoolReportsCount = 0L;
        UUID correlationID = UUID.randomUUID();
        try {
            String accessToken = getAccessToken();
            schoolReportsCount = this.webClient.post()
                    .uri(String.format(constants.getGradSchoolReportsCountUrl(), reportType))
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(finalSchoolDistricts))
                    .retrieve().bodyToMono(Long.class).block();
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve school reports counts", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve schools reports counts", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} of {} school reports for archiving of SoR: {}", schoolReportsCount, reportType, String.join(",", finalSchoolDistricts));
        }
        return schoolReportsCount;
    }

    public List<UUID> getReportStudentIDsByStudentIDsAndReportType(List<String> finalSchoolDistricts, String reportType, DistributionSummaryDTO summaryDTO) {
        List<UUID> result = new ArrayList<>();
        UUID correlationID = UUID.randomUUID();
        try {
            String accessToken = getAccessToken();
            final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
            };
            result = this.webClient.post()
                    .uri(String.format(constants.getGradStudentReportsGuidsUrl(), reportType))
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(finalSchoolDistricts))
                    .retrieve().bodyToMono(responseType).block();
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve report student guids", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve schools reports counts", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} of {} school reports for archiving of SoR: {}", result.size(), reportType, String.join(",", finalSchoolDistricts));
        }
        return result;
    }

    public Integer archiveSchoolReports(Long batchId, List<String> finalSchoolDistricts, String reportType, DistributionSummaryDTO summaryDTO) {
        UUID correlationID = UUID.randomUUID();
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Archive {} School Reports for Ministry Codes: {}", reportType, !finalSchoolDistricts.isEmpty() ? String.join(",", finalSchoolDistricts) : summaryDTO.getSchools().stream().map(School::getMincode).collect(Collectors.joining(",")));
        }
        try {
            String accessToken = getAccessToken();
            return this.webClient.post()
                    .uri(String.format(constants.getGradArchiveSchoolReportsUrl(), batchId, reportType))
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(finalSchoolDistricts))
                    .retrieve().bodyToMono(Integer.class).block();
        } catch(Exception e) {
            LOGGER.error("Unable to archive School Reports", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to archive School Reports", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
            return 0;
        }
    }

    public Long getTotalStudentsBySchoolOfRecordAndStudentStatus(List<String> finalSchoolDistricts, String studentStatus, DistributionSummaryDTO summaryDTO) {
        Long studentsCount = 0L;
        UUID correlationID = UUID.randomUUID();
        try {
            String accessToken = getAccessToken();
            studentsCount = this.webClient.post()
                    .uri(String.format(constants.getGradStudentCountUrl(), studentStatus))
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(finalSchoolDistricts))
                    .retrieve().bodyToMono(Long.class).block();
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve student counts", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve student counts", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} of {} students for archiving of SoR: {}", studentsCount, studentStatus, String.join(",", finalSchoolDistricts));
        }
        return studentsCount;
    }

    public Integer archiveStudents(Long batchId, List<String> finalSchoolDistricts, String studentStatus, DistributionSummaryDTO summaryDTO) {
        UUID correlationID = UUID.randomUUID();
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Archive {} Students for Ministry Codes: {}", studentStatus, String.join(",", finalSchoolDistricts));
        }
        try {
            String accessToken = getAccessToken();
            return this.webClient.post()
                    .uri(String.format(constants.getGradArchiveStudentsUrl(), batchId, studentStatus))
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(finalSchoolDistricts))
                    .retrieve().bodyToMono(Integer.class).block();
        } catch(Exception e) {
            LOGGER.error("Unable to archive Students", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to archive Students", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
            return 0;
        }
    }

    public List<UUID> getStudentIDsBySearchCriteriaOrAll(StudentSearchRequest searchRequest, DistributionSummaryDTO summaryDTO) {
        UUID correlationID = UUID.randomUUID();
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Get Students for Ministry Codes: {}", String.join(",", searchRequest.getSchoolOfRecords()));
        }
        try {
            String accessToken = getAccessToken();
            final ParameterizedTypeReference<List<UUID>> responseType = new ParameterizedTypeReference<>() {
            };
            return this.webClient.post()
                    .uri(constants.getGradGetStudentsBySearchCriteriaUrl())
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(searchRequest))
                    .retrieve().bodyToMono(responseType).block();
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve list of Students", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve list of Students", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    public long deleteStudentReports(Long batchId, List<UUID> uuids, String reportType, DistributionSummaryDTO summaryDTO) {
        Long studentsCount = 0L;
        UUID correlationID = UUID.randomUUID();
        try {
            String accessToken = getAccessToken();
            studentsCount = this.webClient.post()
                    .uri(String.format(constants.getDeleteStudentReportsUrl(), batchId, reportType))
                    .headers(h -> { h.setBearerAuth(accessToken); h.set(EducGradBatchGraduationApiConstants.CORRELATION_ID, correlationID.toString()); })
                    .body(BodyInserters.fromValue(uuids))
                    .retrieve().bodyToMono(Long.class).block();
        } catch(Exception e) {
            LOGGER.error("Unable to delete student reports", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to delete student reports", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} of {} student reports for deleting", studentsCount, reportType);
        }
        return studentsCount;
    }
}
