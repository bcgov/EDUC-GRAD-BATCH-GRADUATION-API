package ca.bc.gov.educ.api.batchgraduation.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Getter
@Setter
public class EducGradBatchGraduationApiConstants {

    // **** API Mappings
	public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String CORRELATION_ID = "correlationID";
    public static final String GRAD_BATCH_API_ROOT_MAPPING = "/api/" + API_VERSION + "/batch";

    // Manual Run
    public static final String EXECUTE_REG_GRAD_BATCH_JOB = "/executereggradbatchjob";
    public static final String EXECUTE_TVR_RUN_BATCH_JOB = "/executetvrrunbatchjob";
    public static final String EXECUTE_DIS_RUN_BATCH_JOB = "/executedisrunbatchjob";
    public static final String EXECUTE_MONTHLY_DIS_RUN_BATCH_JOB = "/executemonthlydisrunbatchjob";
    public static final String EXECUTE_YEARLY_DIS_RUN_BATCH_JOB = "/executeyearlydisrunbatchjob";
    public static final String EXECUTE_SUPP_DIS_RUN_BATCH_JOB = "/executesuppdisrunbatchjob";
    public static final String EXECUTE_YEARLY_NON_GRAD_DIS_RUN_BATCH_JOB = "/executenongraddisrunbatchjob";
    public static final String EXECUTE_CERT_REGEN_BATCH_JOB = "/executecertregenbatchjob";
    public static final String EXECUTE_EDW_SNAPSHOT_BATCH_JOB = "/executeedwsnapshotbatchjob";
    public static final String EXECUTE_ARCHIVE_SCHOOL_REPORTS_RUN_BATCH_JOB = "/report/school/archive";
    public static final String EXECUTE_DELETE_STUDENT_REPORTS_RUN_BATCH_JOB = "/report/student/delete";
    public static final String EXECUTE_YEARLY_ARCHIVE_STUDENTS_RUN_BATCH_JOB = "/student/archive";

    // Special Run
    public static final String EXECUTE_SPECIALIZED_RUNS = "/specialrun";
    public static final String EXECUTE_SPECIALIZED_TVR_RUNS = "/tvrspecialrun";
    public static final String EXECUTE_SPECIALIZED_USER_REQ_RUNS = "/userrequestdisrun/{credentialType}";
    public static final String EXECUTE_SPECIALIZED_BLANK_USER_REQ_RUNS = "/userrequestblankdisrun/{credentialType}";
    public static final String EXECUTE_SPECIALIZED_PSI_USER_REQ_RUNS = "/executepsireportbatchjob/{transmissionType}";

    // Re-Run
    public static final String EXECUTE_RE_RUNS_ALL = "/rerun/all/{batchId}";
    public static final String EXECUTE_RE_RUNS_FAILED = "/rerun/failed/{batchId}";
    public static final String EXECUTE_REGEN_SCHOOL_REPORTS = "/regenerate/school-report/{batchId}";
    public static final String EXECUTE_REGEN_SCHOOL_REPORTS_BY_REQUEST = "/regenerate/school-report";
    public static final String EXECUTE_REGEN_STUDENT_REPORTS_BY_REQUEST = "/regenerate/student-report";

    public static final String BATCH_JOB_TYPES_MAPPING = "/batchjobtype";
    public static final String BATCH_JOB_TYPE_MAPPING = "/batchjobtype/{batchJobTypeCode}";

    public static final String BATCH_DASHBOARD = "/dashboard";
    public static final String BATCH_ERRORS = "/dashboard/errors/{batchId}";
    public static final String BATCH_SUMMARY = "/dashboard/summary";

    public static final String SCHEDULE_JOBS = "/schedule/add";
    public static final String REMOVE_JOB = "/schedule/remove/{jobId}";
    public static final String LIST_JOBS = "/schedule/listjobs";

    public static final String PROCESSING_LIST = "/processing/all";
    public static final String UPDATE_ENABLED = "/processing/toggle/{jobType}";

    public static final String LOAD_STUDENT_IDS = "/loadstudentIds";

    public static final String UPDATE_PARAM = "/parameters";

    // **** Notify the distribution job is completed, and update its status(batch job & student) back
    public static final String NOTIFY_DISTRIBUTION_JOB_IS_COMPLETED = "/notifyDistributionJobCompleted";
       
    // **** Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String SECOND_DEFAULT_DATE_FORMAT = "yyyy/MM/dd";
    public static final String SECOND_DEFAULT_DATE_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    public static final String DATE_FORMAT = SECOND_DEFAULT_DATE_FORMAT;
    public static final String TRAX_DATE_FORMAT = "yyyyMM";

    // **** Model defaults
    public static final String DEFAULT_CREATED_BY = "API_GRAD_BATCH";
    protected static final Date DEFAULT_CREATED_TIMESTAMP = new Date();
    public static final String DEFAULT_UPDATED_BY = "API_GRAD_BATCH";
    protected static final Date DEFAULT_UPDATED_TIMESTAMP = new Date();

    // **** Batch Parameters
    public static final String JOB_TRIGGER = "jobTrigger";
    public static final String JOB_TYPE = "jobType";
    public static final String SEARCH_REQUEST = "searchRequest";
    public static final String TRANSMISSION_TYPE = "transmissionType";
    public static final String USER_SCHEDULED = "userScheduled";

    public static final String TVRCREATE = "tvrCreated";
    public static final String TVRUPDATE = "tvrUpdated";
    public static final String TVRDELETE = "tvrDeleted";
    public static final String ALL = "all";

    @Value("${authorization.user}")
    private String userName;

    @Value("${authorization.password}")
    private String password;

    @Value("${endpoint.keycloak.getToken}")
    private String tokenUrl;

    @Value("${endpoint.grad-graduation-api.graduatestudent.url}")
    private String graduationApiUrl;
    
    @Value("${endpoint.grad-graduation-api.reportonlyrun.url}")
    private String graduationApiReportOnlyUrl;

    @Value("${endpoint.grad-graduation-api.tvrrun.url}")
    private String graduationApiProjectedGradUrl;

    @Value("${endpoint.grad-student-api.get-student-for-batch-input}")
    private String gradStudentApiGradStatusForBatchUrl;

    @Value("${endpoint.grad-student-api.pen-student-by-pen}")
    private String penStudentApiByPenUrl;

    @Value("${endpoint.grad-student-api.grad-status}")
    private String gradStudentApiGradStatusUrl;

    @Value("${endpoint.grad-student-api.student-for-grad-list}")
    private String gradStudentApiStudentForGradListUrl;

    @Value("${endpoint.grad-student-api.student-for-projectedgrad-list}")
    private String gradStudentApiStudentForProjectedGradListUrl;

    @Value("${endpoint.grad-student-api.student-for-special-grad-run-list}")
    private String gradStudentApiStudentForSpcGradListUrl;

    @Value("${endpoint.grad-student-api.get-student-data-list}")
    private String gradStudentApiStudentDataListUrl;

    @Value("${endpoint.grad-graduation-report-api.get-school-reports-count.url}")
    private String gradSchoolReportsCountUrl;

    @Value("${endpoint.grad-graduation-report-api.get-student-reports-guid.url}")
    private String gradStudentReportsGuidsUrl;

    @Value("${endpoint.grad-graduation-report-api.archive-school-reports.url}")
    private String gradArchiveSchoolReportsUrl;

    @Value("${endpoint.grad-student-api.get-students-count}")
    private String gradStudentCountUrl;

    @Value("${endpoint.grad-student-api.archive-students}")
    private String gradArchiveStudentsUrl;

    @Value("${endpoint.grad-student-api.student-by-search-criteria}")
    private String gradGetStudentsBySearchCriteriaUrl;

    @Value("${endpoint.grad-graduation-report-api.check-sccp-certificate-exists}")
    private String checkSccpCertificateExists;

    @Value("${endpoint.grad-trax-api.get-edw-snapshot-schools.url}")
    private String edwSnapshotSchoolsUrl;

    @Value("${endpoint.grad-trax-api.get-edw-snapshot-students-by-min-code.url}")
    private String edwSnapshotStudentsByMincodeUrl;

    @Value("${endpoint.grad-graduation-api.snapshot-graduation-status-for-edw.url}")
    private String snapshotGraduationStatusForEdwUrl;

    // Number of Partitions
    @Value("${batch.partitions.number}")
    private int numberOfPartitions;

    // Spring Batch Transaction Chunk Size
    @Value("${batch.transaction.chunk-size}")
    private int transactionChunkSize;

    // Token expiry offset (seconds)
    @Value("${batch.token-expiry.offset}")
    private int tokenExpiryOffset;

    @Value("${endpoint.grad-graduation-report-api.get-transcript-list.url}")
    private String transcriptDistributionList;

    @Value("${endpoint.grad-graduation-report-api.get-transcript-list.yearly.url}")
    private String transcriptYearlyDistributionList;

    @Value("${endpoint.grad-graduation-report-api.get-certificate-list.url}")
    private String certificateDistributionList;

    @Value("${endpoint.grad-student-api.get-student-record}")
    private String studentInfo;

    @Value("${endpoint.grad-distribution-api.merge-n-upload.url}")
    private String mergeAndUpload;

    @Value("${endpoint.grad-distribution-api.merge-psi-n-upload.url}")
    private String mergePsiAndUpload;

    @Value("${endpoint.grad-distribution-api.merge-n-upload-yearly.url}")
    private String mergeAndUploadYearly;

    @Value("${endpoint.grad-distribution-api.merge-n-upload-supplemental.url}")
    private String mergeAndUploadSupplemental;

    @Value("${endpoint.grad-distribution-api.reprint-n-upload.url}")
    private String reprintAndUpload;

    @Value("${endpoint.grad-distribution-api.blanks-n-upload.url}")
    private String createBlanksAndUpload;

    @Value("${endpoint.grad-distribution-api.posting-distribution.url}")
    private String postingDistribution;

    @Value("${endpoint.grad-graduation-report-api.update-student-credential.url}")
    private String updateStudentCredential;

    @Value("${endpoint.grad-graduation-report-api.get-certificate-types.url}")
    private String certificateTypes;

    @Value("${endpoint.grad-graduation-report-api.get-school-reports-lite-by-report-type.url}")
    private String schoolReportsLiteByReportTypeUrl;

    @Value("${endpoint.grad-student-api.update-student-record}")
    private String updateStudentRecord;

    @Value("${endpoint.grad-student-api.update-student-record-history}")
    private String updateStudentRecordHistory;

    @Value("${endpoint.grad-student-api.get-student-data-nongrad-yearly}")
    private String studentDataNonGradEarlyByMincode;

    @Value("${endpoint.grad-student-api.get-school-data-nongrad-yearly}")
    private String schoolDataNonGradEarly;

    @Value("${endpoint.grad-student-api.get-district-data-nongrad-yearly}")
    private String districtDataNonGradEarly;

    @Value("${endpoint.grad-student-api.read-student-data-nongrad-yearly}")
    private String studentDataNonGradEarly;

    @Value("${endpoint.grad-graduation-report-api.get-district-data-yearly.url}")
    private String districtDataYearly;

    @Value("${endpoint.grad-graduation-api.student-data-yearly.url}")
    private String studentReportDataYearly;

    @Value("${endpoint.grad-graduation-report-api.user-req-dis-run.url}")
    private String studentDataForUserReqDisRun;

    @Value("${endpoint.grad-graduation-report-api.user-req-dis-run-for-not-yet-distributed.url}")
    private String studentDataForUserReqDisRunWithNullDistributionDate;

    @Value("${endpoint.grad-graduation-api.schoolreport.url}")
    private String createAndStoreSchoolReports;

    @Value("${endpoint.grad-graduation-api.school_year_end_report.url}")
    private String schoolYearEndReport;

    @Value("${endpoint.grad-graduation-api.school_month_report.url}")
    private String schoolMonthReport;

    @Value("${endpoint.grad-graduation-api.district_year_end_report.url}")
    private String districtYearEndReport;

    @Value("${endpoint.grad-graduation-api.district_month_report.url}")
    private String districtMonthReport;

    @Value("${endpoint.grad-graduation-api.school_district_year_end_report.url}")
    private String schoolDistrictYearEndReport;

    @Value("${endpoint.grad-graduation-api.school_district_month_report.url}")
    private String schoolDistrictMonthReport;

    @Value("${endpoint.grad-distribution-api.read-n-post.url}")
    private String readAndPost;

    @Value("${endpoint.grad-student-api.read-grad-student-record}")
    private String readGradStudentRecord;

    @Value("${endpoint.grad-student-api.read-grad-student-record-batch}")
    private String readGradStudentRecordBatch;

    @Value("${endpoint.grad-graduation-report-api.get-school-report-list.url}")
    private String schoolReportPostingList;

    @Value("${endpoint.grad-graduation-report-api.update-school-report.url}")
    private String updateSchoolReport;

    @Value("${endpoint.grad-graduation-report-api.update-student-report.url}")
    private String updateStudentReport;

    @Value("${endpoint.grad-graduation-report-api.delete-student-report.url}")
    private String deleteStudentReportsUrl;

    @Value("${endpoint.grad-trax-api.get-psi-student-list.url}")
    private String psiStudentList;

    @Value("${endpoint.grad-student-api.update-flag-ready-for-batch}")
    private String updateStudentFlagReadyForBatchByStudentIDs;

    @Value("${endpoint.grad-graduation-api.student-certificate-regeneration.url}")
    private String studentCertificateRegeneration;

    @Value("${endpoint.grad-trax-api.commonschool-by-mincode.url}")
    private String commonSchoolByMincode;

    @Value("${endpoint.grad-trax-api.school-by-min-code.url}")
    private String traxSchoolByMincode;

    @Value("${endpoint.grad-trax-api.district-by-school-category.url}")
    private String traxDistrictBySchoolCategory;

    @Value("${endpoint.grad-trax-api.school-by-school-category.url}")
    private String traxSchoolBySchoolCategory;

    @Value("${endpoint.grad-trax-api.school-by-district-code.url}")
    private String traxSchoolByDistrict;

    @Value("${endpoint.grad-student-api.get-deceased-student-id-list}")
    private String deceasedStudentIDList;

    // Resilience
    @Value("${resilience.retry.default.maxAttempts}")
    private int defaultRetryMaxAttempts;

    @Value("${resilience.retry.default.waitDuration}")
    private int defaultRetryWaitDurationSeconds;

    @Value("${resilience.retry.get-token.maxAttempts}")
    private int tokenRetryMaxAttempts;

    @Value("${resilience.retry.get-token.waitDuration}")
    private int tokenRetryWaitDurationSeconds;

    // Splunk LogHelper Enabled
    @Value("${splunk.log-helper.enabled}")
    private boolean splunkLogHelperEnabled;

    @Value("${batch.purge-old-records.staleInDays}")
    private int recordsStaleInDays;

}
