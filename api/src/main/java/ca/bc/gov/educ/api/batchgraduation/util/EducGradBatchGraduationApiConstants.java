package ca.bc.gov.educ.api.batchgraduation.util;

public class EducGradBatchGraduationApiConstants {

	public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String GRAD_BATCH_API_ROOT_MAPPING = "/api/" + API_VERSION + "/batch";
    public static final String EXECUTE_BATCH_JOB = "/executebatchjob";
    public static final String LOAD_STUDENT_IDS = "/loadstudentIds";

    // Data Conversion
    public static final String GRAD_CONVERSION_API_MAPPING = "/dataconversion";
    public static final String EXECUTE_GRAD_STUDENT_CONVERSION_JOB = "/gradStudent";
    public static final String EXECUTE_COURSE_RESTRICTIONS_CONVERSION_JOB = "/courseRestrictions";
       
    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    
    public static final String TRAX_DATE_FORMAT = "yyyyMM";
	public static final String ENDPOINT_GET_TOKEN_URL = "${endpoint.keycloack.getToken}";
	public static final String ENDPOINT_RUN_GRADUATION_API_URL="${endpoint.grad-graduation-api.url}";
	 public static final String ENDPOINT_PEN_STUDENT_API_BY_PEN_URL="${endpoint.pen-student-api.by-pen.url}";
	 public static final String ENDPOINT_GRAD_STUDENT_API_URL="${endpoint.graduation-status-api.read-grad-status.url}";
	 public static final String ENDPOINT_GRAD_STATUS_UPDATE_URL = "${endpoint.graduation-status-api.update-grad-status}";
}
