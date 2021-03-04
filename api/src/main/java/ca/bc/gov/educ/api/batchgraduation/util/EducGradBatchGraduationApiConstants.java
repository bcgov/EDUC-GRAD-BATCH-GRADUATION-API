package ca.bc.gov.educ.api.batchgraduation.util;

public class EducGradBatchGraduationApiConstants {

	public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String GRAD_BATCH_API_ROOT_MAPPING = "/api/" + API_VERSION + "/batch";
    public static final String EXECUTE_BATCH_JOB = "/executebatchjob";
       
    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    
    public static final String TRAX_DATE_FORMAT = "yyyyMM";
	public static final String ENDPOINT_GET_TOKEN_URL = "${endpoint.keycloack.getToken}";
	public static final String ENDPOINT_RUN_GRADUATION_API_URL="${endpoint.grad-graduation-api.url}";
}
