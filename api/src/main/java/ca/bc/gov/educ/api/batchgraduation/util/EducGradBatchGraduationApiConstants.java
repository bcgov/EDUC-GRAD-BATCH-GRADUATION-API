package ca.bc.gov.educ.api.batchgraduation.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class EducGradBatchGraduationApiConstants {

	public static final String API_ROOT_MAPPING = "";
    public static final String API_VERSION = "v1";
    public static final String GRAD_BATCH_API_ROOT_MAPPING = "/api/" + API_VERSION + "/batch";
    public static final String EXECUTE_BATCH_JOB = "/executebatchjob";
    public static final String LOAD_STUDENT_IDS = "/loadstudentIds";

    // Data Conversion
    public static final String EXECUTE_DATA_CONVERSION_BATCH_JOB = "/executeGradStudentDataConversionJob";
    public static final String GRAD_CONVERSION_API_MAPPING = "/dataconversion";
    public static final String EXECUTE_COURSE_RESTRICTIONS_CONVERSION_JOB = "/courseRestrictions";
       
    //Default Date format constants
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    
    public static final String TRAX_DATE_FORMAT = "yyyyMM";

    @Value("${authorization.user}")
    private String userName;

    @Value("${authorization.password}")
    private String password;

    @Value("${endpoint.keycloack.getToken}")
    private String tokenUrl;

    @Value("${endpoint.grad-graduation-api.url}")
    private String graduationApiUrl;

    @Value("${endpoint.grad-student-api.pen-student-by-pen}")
    private String penStudentApiByPenUrl;

    @Value("${endpoint.grad-student-api.grad-status}")
    private String gradStudentApiGradStatusUrl;

    @Value("${endpoint.grad-student-api.student-optional-program}")
    private String gradStudentApiStudentOptionalProgramUrl;

    @Value("${endpoint.grad-student-api.update-student-optional-program}")
    private String gradStudentApiUpdateStudentOptionalProgramUrl;

    @Value("${endpoint.grad-program-api.optional-program}")
    private String gradProgramApiOptionalProgramUrl;

    @Value("${endpoint.grad-student-api.student-for-grad-list}")
    private String gradStudentApiStudentForGradListUrl;

    @Value("${endpoint.grad-course-api.course-restriction}")
    private String courseRestrictionUrl;

    @Value("${endpoint.grad-course-api.check-french-immersion-course}")
    private String checkFrenchImmersionCourseUrl;
}
