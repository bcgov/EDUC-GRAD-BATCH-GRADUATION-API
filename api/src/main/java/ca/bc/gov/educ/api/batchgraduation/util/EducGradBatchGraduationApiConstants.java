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
    public static final String GRAD_BATCH_API_ROOT_MAPPING = "/api/" + API_VERSION + "/batch";
    public static final String EXECUTE_REG_GRAD_BATCH_JOB = "/executereggradbatchjob";
    public static final String EXECUTE_TVR_RUN_BATCH_JOB = "/executetvrrunbatchjob";
    public static final String LOAD_STUDENT_IDS = "/loadstudentIds";
    public static final String EXECUTE_SPECIALIZED_RUNS = "/specialrun";
    
    public static final String BATCH_DASHBOARD = "/dashboard";

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

}
