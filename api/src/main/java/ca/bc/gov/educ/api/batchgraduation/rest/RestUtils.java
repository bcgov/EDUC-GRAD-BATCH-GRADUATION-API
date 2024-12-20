package ca.bc.gov.educ.api.batchgraduation.rest;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.GraduationReportService;
import ca.bc.gov.educ.api.batchgraduation.util.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
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

    final EducGradBatchGraduationApiConstants constants;

    final RESTService restService;

    final JsonTransformer jsonTransformer;

    final GraduationReportService graduationReportService;

    final TokenUtils tokenUtils;

    @Autowired
    public RestUtils(final GraduationReportService graduationReportService,
                     final EducGradBatchGraduationApiConstants constants, final JsonTransformer jsonTransformer,
                     final RESTService restService, final TokenUtils tokenUtils) {
        this.constants = constants;
        this.restService = restService;
        this.tokenUtils = tokenUtils;
        this.graduationReportService = graduationReportService;
        this.jsonTransformer = jsonTransformer;
    }

    public List<Student> getStudentsByPen(String pen) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        String url = String.format(constants.getPenStudentApiByPenUrl(), pen);
        LOGGER.debug(URL_FORMAT_STR, url);
        var response = restService.get(url, List.class);
        if (response != null && !response.isEmpty()) {
            return jsonTransformer.convertValue(response, new TypeReference<>() {});
        }
        return new ArrayList<>();
    }

    public AlgorithmResponse runGradAlgorithm(UUID studentID, String gradProgram, String programCompleteDate, Long batchId) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        String url = isReportOnly(studentID, gradProgram, programCompleteDate)?
            String.format(constants.getGraduationApiReportOnlyUrl(), studentID, batchId) : String.format(constants.getGraduationApiUrl(), studentID, batchId);
        return restService.get(url, AlgorithmResponse.class);
    }

    public AlgorithmResponse runProjectedGradAlgorithm(UUID studentID, Long batchId) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        return restService.get(String.format(constants.getGraduationApiProjectedGradUrl(), studentID,batchId), AlgorithmResponse.class);
    }

    public BatchGraduationStudentRecord runGetStudentForBatchInput(UUID studentID) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        return restService.get(String.format(constants.getGradStudentApiGradStatusForBatchUrl(), studentID), BatchGraduationStudentRecord.class);
    }

    public BatchGraduationStudentRecord getStudentForBatchInput(UUID studentID, AlgorithmSummaryDTO summary) {
        LOGGER.debug(STUDENT_READ,studentID);
        try {
            return this.runGetStudentForBatchInput(studentID);
        } catch(Exception e) {
            summary.updateError(studentID,"GRAD-STUDENT-API IS DOWN","GRAD Student API is unavailable at this moment");
            LOGGER.info("GET Failed STU-ID:{} Errors:{}",studentID,summary.getErrors().size());
            return null;
        }
    }

    public List<StudentCredentialDistribution> fetchDistributionRequiredDataStudentsNonGradYearly() {
        return graduationReportService.getStudentsNonGradForYearlyDistribution(fetchAccessToken());
    }

    public List<StudentCredentialDistribution> fetchDistributionRequiredDataStudentsNonGradYearly(String mincode) {
        return graduationReportService.getStudentsNonGradForYearlyDistribution(mincode, fetchAccessToken());
    }

    public List<StudentCredentialDistribution> fetchDistributionRequiredDataStudentsYearly() {
        return graduationReportService.getStudentsForYearlyDistribution(fetchAccessToken());
    }


    public Integer runRegenerateStudentCertificate(String pen) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        String url = constants.getStudentCertificateRegeneration() + "?isOverwrite=%s";
        return restService.get(String.format(url, pen, "N"), Integer.class);
    }

    public List<UUID> getStudentsForAlgorithm() {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        var response = restService.get(constants.getGradStudentApiStudentForGradListUrl(), List.class);
        return jsonTransformer.convertValue(response, new TypeReference<>(){});
    }

    public List<UUID> getStudentsForProjectedAlgorithm() {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        var response =  restService.get(constants.getGradStudentApiStudentForProjectedGradListUrl(), List.class);
        return jsonTransformer.convertValue(response, new TypeReference<>(){});
    }

    // EDUC-GRAD-STUDENT-API ========================================

    public GraduationStudentRecord saveGraduationStudentRecord(GraduationStudentRecord graduationStudentRecord) {
        return restService.post(String.format(constants.getGradStudentApiGradStatusUrl(),graduationStudentRecord.getStudentID()),
                graduationStudentRecord, GraduationStudentRecord.class);
    }

    public List<UUID> getStudentsForSpecialGradRun(StudentSearchRequest req) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        GraduationStudentRecordSearchResult res = restService.post(constants.getGradStudentApiStudentForSpcGradListUrl(), req, GraduationStudentRecordSearchResult.class);
        return res != null ?res.getStudentIDs() : new ArrayList<>();
    }

    public GraduationStudentRecord processStudent(GraduationStudentRecord item, AlgorithmSummaryDTO summary) {
        LOGGER.debug(STUDENT_PROCESS,item.getStudentID());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            AlgorithmResponse algorithmResponse = this.runGradAlgorithm(item.getStudentID(),
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

    public boolean isReportOnly(UUID studentID, String gradProgram, String programCompletionDate) {
        boolean isFMR = false;
        if ("SCCP".equalsIgnoreCase(gradProgram)) {
            if (programCompletionDate != null) {
                Date pCD = EducGradBatchGraduationApiUtils.parsingTraxDate(programCompletionDate);
                int diff = EducGradBatchGraduationApiUtils.getDifferenceInDays(EducGradBatchGraduationApiUtils.getProgramCompletionDate(pCD), EducGradBatchGraduationApiUtils.getCurrentDate());
                if (diff >= 0) {
                    isFMR = checkSccpCertificateExists(studentID);
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
            AlgorithmResponse algorithmResponse = this.runProjectedGradAlgorithm(item.getStudentID(), summary.getBatchId());
            return processGraduationStudentRecord(item, summary, algorithmResponse);
        } catch(Exception e) {
            summary.updateError(item.getStudentID(),GRADUATION_API_IS_DOWN,GRADUATION_API_DOWN_MSG);
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info(FAILED_STUDENT_ERROR_MSG,item.getStudentID(),summary.getErrors().size());
            return null;
        }
    }

    public Integer getStudentByPenFromStudentAPI(List<LoadStudentData> loadStudentData) {
       AtomicReference<Integer> recordsAdded = new AtomicReference<>(0);
        loadStudentData.forEach(student -> {
            List<Student> stuDataList = this.getStudentsByPen(student.getPen());
            stuDataList.forEach(st-> {
                GraduationStudentRecord gradStu = new GraduationStudentRecord();
                gradStu.setProgram(student.getProgramCode());
                gradStu.setSchoolOfRecord(student.getSchool());
                gradStu.setStudentGrade(student.getStudentGrade());
                gradStu.setRecalculateGradStatus("Y");
                gradStu.setStudentStatus(student.getStudentStatus());
                gradStu.setStudentID(UUID.fromString(st.getStudentID()));
                saveGraduationStudentRecord(gradStu);
                recordsAdded.getAndSet(recordsAdded.get() + 1);
            });
        });
        return recordsAdded.get();
    }

    public List<GraduationStudentRecord> getStudentData(List<UUID> studentIds) {
        StudentList stuList = new StudentList();
        stuList.setStudentids(studentIds);
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        var response = restService.post(constants.getGradStudentApiStudentDataListUrl(), stuList, List.class);
        return jsonTransformer.convertValue(response, new TypeReference<>(){});
    }

    public StudentCredentialDistribution processDistribution(StudentCredentialDistribution item, DistributionSummaryDTO summary, boolean useSchoolAtGrad) {
        LOGGER.info(STUDENT_PROCESS,item.getStudentID());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        StudentCredentialDistribution scObj = summary.getGlobalList().stream().filter(pr -> pr.getStudentID().compareTo(item.getStudentID()) == 0)
                .findAny()
                .orElse(null);
        if(scObj != null) {
            item.setSchoolId(scObj.getSchoolId());
            item.setDistrictId(scObj.getDistrictId());
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
                    item.setSchoolId(stuRec.getSchoolAtGradId() == null? stuRec.getSchoolOfRecordId() : stuRec.getSchoolAtGradId());
                } else {
                    item.setSchoolOfRecord(stuRec.getSchoolOfRecord());
                    item.setSchoolId(stuRec.getSchoolOfRecordId());
                }
                ca.bc.gov.educ.api.batchgraduation.model.institute.School school  = getSchool(item.getSchoolId());
                if (school != null) {
                    item.setDistrictId(UUID.fromString(school.getDistrictId()));
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
        try {
            List<Student>  stuDataList = this.getStudentsByPen(item.getPen());
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
        String credentialType = summary.getCredentialType();
        if (credentialType != null && credentialType.equalsIgnoreCase("OC")){
            GradCertificateTypes certType = this.getCertTypes(item.getCredentialTypeCode());
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

    public GradCertificateTypes getCertTypes(String certType) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        var result = restService.get(String.format(constants.getCertificateTypes(),certType), GradCertificateTypes.class);
        if(result != null)
            LOGGER.info("Fetched {} Cert type Records : ",result.getCode());

        return result;
    }

    public GraduationStudentRecord getStudentDataForBatch(String studentID) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        var result = restService.get(String.format(constants.getStudentInfo(),studentID), GraduationStudentRecord.class);
        if(result != null)
            LOGGER.info("Fetched {} Graduation Records",result.getStudentID());

        return result;
    }

    public GraduationStudentRecordDistribution getStudentData(String studentID) {
        String url = String.format(constants.getStudentInfo(),studentID);
        var result = restService.get(url, GraduationStudentRecordDistribution.class);
        if(result != null)
            LOGGER.info("Fetched {} Graduation Records",result.getStudentID());
        return result;
    }

    public ca.bc.gov.educ.api.batchgraduation.model.institute.School getSchool(UUID schoolId) {
        String url = String.format(constants.getSchoolBySchoolId(),schoolId);
        var result = restService.get(url, ca.bc.gov.educ.api.batchgraduation.model.institute.School.class);
        if(result != null)
            LOGGER.info("Fetched {} Institute School",result.getSchoolId());
        return result;
    }

    public Integer createAndStoreSchoolReports(List<UUID> uniqueSchools, String type) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
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
            List<UUID> schoolIds = uniqueSchools.subList(startIndex, endIndex);
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating School Reports for schools: {}", schoolIds.size());
            }
            result += restService.post(String.format(constants.getCreateAndStoreSchoolReports(),type), schoolIds, Integer.class);
        }
        LOGGER.info("Created and Stored {} School Reports", result);
        return result;
    }

    public Integer createAndStoreSchoolReports(String minCode, String reportType, SchoolReportsRegenSummaryDTO summaryDTO) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        Integer result = 0;
        try {
            if (minCode == null || minCode.isEmpty()) {
                LOGGER.info("{} Schools selected for School Reports Regeneration", result);
                return result;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Creating School Reports for school {}", minCode);
            }
            result += restService.post(String.format(constants.getCreateAndStoreSchoolReports(), reportType), List.of(minCode), Integer.class);
            LOGGER.info("Created and Stored {} School Reports", result);
            // When multiple reports are generated, the count is > 1. In this case, still return 1 so that the actual processed
            // mincodes is only incremented by 1 and not by the number of school reports generated.
            if (result > 1)
                result = 1;
        } catch(Exception e) {
            LOGGER.error("Unable to Regenerate School Reports", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to Regenerate School Reports", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
            return 0;
        }
        return result;
    }

    public Integer processStudentReports(List<UUID> uuidList, String studentReportType) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        Integer result = restService.post(String.format(constants.getUpdateStudentReport(), studentReportType), uuidList, Integer.class, getAccessToken());
        LOGGER.info("{} Student {} Reports", result, studentReportType);
        return result;
    }

    //Grad2-1931 sending transmissionType with the webclient.
    public DistributionResponse mergePsiAndUpload(Long batchId, String accessToken, DistributionRequest distributionRequest,String localDownload, String transmissionType) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        var result = restService.post(String.format(constants.getMergePsiAndUpload(),batchId,localDownload,transmissionType), distributionRequest, DistributionResponse.class, accessToken);
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
        return restService.post(distributionUrl, distributionRequest, DistributionResponse.class, accessToken);
    }

    public Boolean executePostDistribution(DistributionResponse distributionResponse) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        return restService.post(constants.getPostingDistribution(), distributionResponse, Boolean.class, getAccessToken());
    }

    public void createBlankCredentialsAndUpload(Long batchId, String accessToken, DistributionRequest distributionRequest, String localDownload) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        var result = restService.post(String.format(constants.getCreateBlanksAndUpload(),batchId,localDownload), distributionRequest, DistributionResponse.class, accessToken);
        if(result != null)
            LOGGER.info("Create and Upload Success {}",result.getMergeProcessResponse());
    }

    public DistributionResponse createReprintAndUpload(Long batchId, String accessToken, DistributionRequest distributionRequest, String activityCode,String localDownload) {
        distributionRequest.setActivityCode(activityCode);
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        var result = restService.post(String.format(constants.getReprintAndUpload(),batchId,activityCode,localDownload), distributionRequest, DistributionResponse.class, accessToken);
        if(result != null)
            LOGGER.info(MERGE_MSG,result.getMergeProcessResponse());
        return  result;
    }

    public void updateStudentCredentialRecord(UUID studentID, String credentialTypeCode, String paperType,String documentStatusCode,String activityCode,String accessToken) {
        String url = String.format(constants.getUpdateStudentCredential(),studentID,
                credentialTypeCode != null? credentialTypeCode : "",paperType,documentStatusCode,activityCode);
        restService.get(url, Boolean.class, accessToken);
    }

    public void updateSchoolReportRecord(String schoolOfRecord, String reportTypeCode, String accessToken) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        restService.get(String.format(constants.getUpdateSchoolReport(),schoolOfRecord,reportTypeCode), Boolean.class, accessToken);
    }

    public void deleteSchoolReportRecord(String schoolOfRecord, String reportTypeCode) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        restService.delete(String.format(constants.getUpdateSchoolReport(),schoolOfRecord,reportTypeCode), Boolean.class);
    }

    public List<StudentCredentialDistribution> getStudentsForUserReqDisRun(String credentialType, StudentSearchRequest req) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        String accessToken = getAccessToken();
        var response = restService.post(String.format(constants.getStudentDataForUserReqDisRun(),credentialType), req, List.class, accessToken);
        return jsonTransformer.convertValue(response, new TypeReference<>(){});

    }

    public List<StudentCredentialDistribution> getStudentsForUserReqDisRunWithNullDistributionDate(String credentialType, StudentSearchRequest req) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        String accessToken = getAccessToken();
        var response = restService.post(String.format(constants.getStudentDataForUserReqDisRunWithNullDistributionDate(),credentialType), req, List.class, accessToken);
        return jsonTransformer.convertValue(response, new TypeReference<>(){});
    }

    public void updateStudentGradRecord(UUID studentID, Long batchId,String activityCode) {
        //Grad2-1931 not updating the school record if student id does not exist.
        try {
            if (studentID != null) {
                String accessToken = getAccessToken();
                String url = String.format(constants.getUpdateStudentRecord(), studentID, batchId, activityCode);
                restService.post(url, "{}", GraduationStudentRecord.class, accessToken);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to update student record {}", studentID);
        }
    }

    public void updateStudentGradRecordHistory(List<UUID> studentIDs, Long batchId, String userName, String activityCode) {
        try {
            if (batchId != null) {
                String url = String.format(constants.getUpdateStudentRecordHistory(), batchId, userName, activityCode);
                restService.put(url,studentIDs, GraduationStudentRecord.class);
            }
        } catch (Exception e) {
            LOGGER.error("Unable to update student record history {}", e.getLocalizedMessage());
        }
    }

    public String updateStudentFlagReadyForBatch(List<UUID> studentIds, String batchJobType) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        StudentList stuList = new StudentList();
        stuList.setStudentids(studentIds);
        return restService.post(String.format(constants.getUpdateStudentFlagReadyForBatchByStudentIDs(), batchJobType), stuList, String.class);
    }

    public Boolean checkSccpCertificateExists (UUID studentID) {
        String url = constants.getCheckSccpCertificateExists() + "?studentID=%s";
        return restService.get(String.format(url, studentID), Boolean.class);
    }

    public List<String> getEDWSnapshotSchools(Integer gradYear) {
        String accessToken = getAccessToken();
        String url = String.format(constants.getEdwSnapshotSchoolsUrl(), gradYear);
        LOGGER.debug(URL_FORMAT_STR,url);
        var response = restService.get(url, List.class, accessToken);
        return jsonTransformer.convertValue(response, new TypeReference<>(){});
    }

    public List<SnapshotResponse> getEDWSnapshotStudents(Integer gradYear, String mincode) {
        String accessToken = getAccessToken();
        String url = String.format(constants.getEdwSnapshotStudentsByMincodeUrl(), gradYear, mincode);
        LOGGER.debug(URL_FORMAT_STR,url);
        var response = restService.get(url, List.class, accessToken);
        return jsonTransformer.convertValue(response, new TypeReference<>(){});
    }

    public String getAccessToken() {
        return tokenUtils.fetchAccessToken();
    }

    public String fetchAccessToken() {
        return tokenUtils.fetchAccessToken();
    }

    public ResponseObj getTokenResponseObject() {
        return tokenUtils.getTokenResponseObject();
    }

    public List<ca.bc.gov.educ.api.batchgraduation.model.institute.District> getDistrictsBySchoolCategoryCode(String schoolCategoryCode) {
        try {
            String url = String.format(constants.getDistricstBySchoolCategory(), schoolCategoryCode);
            var response = restService.get(url, List.class);
            return jsonTransformer.convertValue(response, new TypeReference<>(){});
        } catch (Exception e) {
            LOGGER.error(TRAX_API_IS_DOWN, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    public List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> getSchoolsBySchoolCategoryCode(String schoolCategoryCode) {
        try {
            String url = String.format(constants.getSchoolsBySchoolCategory(), schoolCategoryCode);
            var response = restService.get(url, List.class);
            return jsonTransformer.convertValue(response, new TypeReference<>(){});
        } catch (Exception e) {
            LOGGER.error(TRAX_API_IS_DOWN, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    public List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> getSchoolsByDistrictId(UUID districtId) {
        try {
            String url = String.format(constants.getSearchSchoolsByDistrictId(), districtId);
            var response = restService.get(url, List.class);
            return jsonTransformer.convertValue(response, new TypeReference<>(){});
        } catch (Exception e) {
            LOGGER.error(TRAX_API_IS_DOWN, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    public List<ca.bc.gov.educ.api.batchgraduation.model.institute.School> getSchoolsByDistrictNumber(String distNo) {
        try {
            String url = String.format(constants.getSearchSchoolsByDistrictNumber(), distNo);
            var response = restService.get(url, List.class);
            return jsonTransformer.convertValue(response, new TypeReference<>(){});
        } catch (Exception e) {
            LOGGER.error(TRAX_API_IS_DOWN, e.getLocalizedMessage());
            return new ArrayList<>();
        }
    }

    public SchoolClob getSchoolClob(String schoolId) {
        return restService.get(String.format(constants.getSchoolClobBySchoolId(), schoolId), SchoolClob.class);
    }

    public List<UUID> getDeceasedStudentIDs(List<UUID> studentIDs) {
        var response = restService.post(constants.getDeceasedStudentIDList(), studentIDs, List.class);
        return jsonTransformer.convertValue(response, new TypeReference<>(){});
    }

    public UUID getStudentIDByPen(String pen) {
        try {
            List<Student>  stuDataList = this.getStudentsByPen(pen);
            if(!stuDataList.isEmpty()) {
                return UUID.fromString(stuDataList.get(0).getStudentID());
            }
        } catch (Exception e) {
            LOGGER.error("Error processing student with pen# {} due to {}", pen, e.getLocalizedMessage());
        }
        return null;
    }

    public EdwGraduationSnapshot processSnapshot(EdwGraduationSnapshot item, EdwSnapshotSummaryDTO summary) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        LOGGER.debug(STUDENT_PROCESS,item.getPen());
        summary.setProcessedCount(summary.getProcessedCount() + 1L);
        try {
            return restService.post(constants.getSnapshotGraduationStatusForEdwUrl(), item, EdwGraduationSnapshot.class);
        } catch(Exception e) {
            summary.updateError(item.getPen(),item.getSchoolOfRecord(),GRADUATION_API_IS_DOWN,GRADUATION_API_DOWN_MSG);
            summary.setProcessedCount(summary.getProcessedCount() - 1L);
            LOGGER.info("Failed STU-PEN:{} Errors:{}",item.getPen(),summary.getErrors().size());
            return null;
        }
    }

    public Long getTotalReportsForProcessing(List<UUID> finalSchoolDistricts, String reportType, DistributionSummaryDTO summaryDTO) {
        Long reportsCount = 0L;
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        try {
            String accessToken = getAccessToken();
            reportsCount = restService.post(String.format(constants.getGradSchoolReportsCountUrl(), reportType), finalSchoolDistricts, Long.class, accessToken);
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve school reports counts", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve schools reports counts", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total {} of {} reports available", reportsCount, reportType);
        }
        return reportsCount;
    }

    public Long getTotalReportsForProcessing(List<UUID> finalSchoolDistricts, String reportType, SchoolReportsRegenSummaryDTO summaryDTO) {
        Long reportsCount = 0L;
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        try {
            String accessToken = getAccessToken();
            reportsCount = restService.post(String.format(constants.getGradSchoolReportsCountUrl(), reportType), finalSchoolDistricts, Long.class, accessToken);
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve school reports counts", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve schools reports counts", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total {} of {} reports available", reportsCount, reportType);
        }
        return reportsCount;
    }

    public List<SchoolReport> getSchoolReportsLiteByReportType(String reportType, SchoolReportsRegenSummaryDTO summaryDTO) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        List<SchoolReport> schoolReportsLite = new ArrayList<>();
        try {
            String accessToken = getAccessToken();
            var response = restService.get(String.format(constants.getSchoolReportsLiteByReportTypeUrl(), reportType), List.class, accessToken);
            if (response != null) {
                schoolReportsLite = jsonTransformer.convertValue(response, new TypeReference<List<SchoolReport>>() {
                });
            }
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve school reports data for ALL", e);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve schools reports data for ALL", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total {} of {} reports available",
                    schoolReportsLite == null || schoolReportsLite.isEmpty() ? 0 : schoolReportsLite.size(), reportType);
        }
        return schoolReportsLite;
    }

    public List<UUID> getReportStudentIDsByStudentIDsAndReportType(List<String> finalSchoolDistricts, String reportType, Integer rowCount, DistributionSummaryDTO summaryDTO) {
        List<UUID> result = new ArrayList<>();
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        try {
            String accessToken = getAccessToken();
            var response = restService.post(String.format(constants.getGradStudentReportsGuidsUrl(), reportType, rowCount), finalSchoolDistricts, List.class, accessToken);
            if (response != null) {
                List<UUID> guids = jsonTransformer.convertValue(response, new TypeReference<>() {});
                result.addAll(guids);
            }
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve report student guids", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve schools reports counts", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total {} of {} reports for processing", result.size(), reportType);
        }
        return result;
    }

    public Integer archiveSchoolReports(Long batchId, List<UUID> finalSchoolDistricts, String reportType, DistributionSummaryDTO summaryDTO) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Archive {} School Reports for Ministry Codes: {}", reportType, !finalSchoolDistricts.isEmpty() ? String.join(",", finalSchoolDistricts.toString()) : summaryDTO.getSchools().stream().map(School::getSchoolId).collect(Collectors.joining(",")));
        }
        try {
            String accessToken = getAccessToken();
            return restService.post(String.format(constants.getGradArchiveSchoolReportsUrl(), batchId, reportType), finalSchoolDistricts, Integer.class, accessToken);
        } catch(Exception e) {
            LOGGER.error("Unable to archive School Reports", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to archive School Reports", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
            return 0;
        }
    }

    public Long getTotalStudentsBySchoolOfRecordIdAndStudentStatus(List<UUID> finalSchoolDistricts, String studentStatus, DistributionSummaryDTO summaryDTO) {
        Long studentsCount = 0L;
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        try {
            String accessToken = getAccessToken();
            studentsCount = restService.post(String.format(constants.getGradStudentCountUrl(), studentStatus), finalSchoolDistricts, Long.class, accessToken);
        } catch(Exception e) {
            LOGGER.error("Unable to retrieve student counts", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to retrieve student counts", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Total {} of {} students for archiving of SoR: {}", studentsCount, studentStatus, String.join(",", finalSchoolDistricts.toString()));
        }
        return studentsCount;
    }

    public Integer archiveStudents(Long batchId, List<UUID> finalSchoolDistricts, String studentStatus, DistributionSummaryDTO summaryDTO) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Archive {} Students for Institute School: {}", studentStatus, String.join(",", finalSchoolDistricts.toString()));
        }
        try {
            String accessToken = getAccessToken();
            String userName = StringUtils.defaultString(summaryDTO.getUserName(), "Batch Archive Process");
            return restService.post(String.format(constants.getGradArchiveStudentsUrl(), batchId, studentStatus, userName), finalSchoolDistricts, Integer.class, accessToken);
        } catch(Exception e) {
            LOGGER.error("Unable to archive Students", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to archive Students", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
            return 0;
        }
    }

    public List<UUID> getStudentIDsBySearchCriteriaOrAll(StudentSearchRequest searchRequest, DistributionSummaryDTO summaryDTO) {
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        try {
            String accessToken = getAccessToken();
            var response = restService.post(constants.getGradGetStudentsBySearchCriteriaUrl(), searchRequest, List.class, accessToken);
            return jsonTransformer.convertValue(response, new TypeReference<>() {});
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
        ThreadLocalStateUtil.setCorrelationID(UUID.randomUUID().toString());
        try {
            String accessToken = getAccessToken();
            studentsCount = restService.post(String.format(constants.getDeleteStudentReportsUrl(), batchId, reportType), uuids, Long.class, accessToken);
        } catch(Exception e) {
            LOGGER.error("Unable to delete student reports", e);
            summaryDTO.setErroredCount(summaryDTO.getErroredCount() + 1);
            summaryDTO.getErrors().add(new ProcessError(null,"Unable to delete student reports", e.getLocalizedMessage()));
            summaryDTO.setException(e.getLocalizedMessage());
        }
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("{} of {} student reports for deleting", studentsCount, reportType);
        }
        return ObjectUtils.defaultIfNull(studentsCount, 0L);
    }
}
