package ca.bc.gov.educ.api.batchgraduation.util;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class EducGradBatchGraduationApiConstants {

	public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String CORRELATION_ID = "correlationID";
    public static final String GRAD_BATCH_API_ROOT_MAPPING = "/api/" + API_VERSION + "/batch";
    public static final String EXECUTE_REG_GRAD_BATCH_JOB = "/executereggradbatchjob";
    public static final String EXECUTE_TVR_RUN_BATCH_JOB = "/executetvrrunbatchjob";
    public static final String EXECUTE_DIS_RUN_BATCH_JOB = "/executedisrunbatchjob";
    public static final String EXECUTE_YEARLY_DIS_RUN_BATCH_JOB = "/executeyearlydisrunbatchjob";
    public static final String LOAD_STUDENT_IDS = "/loadstudentIds";
    public static final String EXECUTE_SPECIALIZED_RUNS = "/specialrun";
    public static final String EXECUTE_SPECIALIZED_TVR_RUNS = "/tvrspecialrun";
    public static final String EXECUTE_SPECIALIZED_USER_REQ_RUNS = "/userrequestdisrun/{credentialType}";
    
    public static final String BATCH_DASHBOARD = "/dashboard";
    public static final String BATCH_ERRORS = "/dashboard/errors/{batchId}";
    public static final String BATCH_SUMMARY = "/dashboard/summary";

    // Data Conversion
    public static final String EXECUTE_DATA_CONVERSION_BATCH_JOB = "/executeGradStudentDataConversionJob";
    public static final String GRAD_CONVERSION_API_MAPPING = "/dataconversion";
    public static final String EXECUTE_COURSE_RESTRICTIONS_CONVERSION_JOB = "/courseRestrictions";
       
    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    
    public static final String TRAX_DATE_FORMAT = "yyyyMM";
    
    public static final String DEFAULT_CREATED_BY = "API_GRAD_BATCH";
    public static final Date DEFAULT_CREATED_TIMESTAMP = new Date();
    public static final String DEFAULT_UPDATED_BY = "API_GRAD_BATCH";
    public static final Date DEFAULT_UPDATED_TIMESTAMP = new Date();

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

    // Number of Partitions
    @Value("${batch.partitions.number}")
    private int numberOfPartitions;

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

    @Value("${endpoint.grad-distribution-api.reprint-n-upload.url}")
    private String reprintAndUpload;

    @Value("${endpoint.grad-graduation-report-api.update-student-credential.url}")
    private String updateStudentCredential;

    @Value("${endpoint.grad-student-api.update-student-record}")
    private String updateStudentRecord;

    @Value("${endpoint.grad-graduation-report-api.user-req-dis-run.url}")
    private String studentDataForUserReqDisRun;

    @Value("${endpoint.grad-graduation-api.schoolreport.url}")
    private String createAndStore;

}
