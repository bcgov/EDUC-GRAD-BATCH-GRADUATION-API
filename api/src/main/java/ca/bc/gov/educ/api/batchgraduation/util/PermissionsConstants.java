package ca.bc.gov.educ.api.batchgraduation.util;

public interface PermissionsConstants {
	String _PREFIX = "hasAuthority('";
	String _SUFFIX = "')";

	String LOAD_STUDENT_IDS = _PREFIX + "SCOPE_LOAD_STUDENT_IDS" + _SUFFIX;
	String LOAD_DASHBOARD = _PREFIX + "SCOPE_LOAD_BATCH_DASHBOARD" + _SUFFIX;
	String RUN_GRAD_ALGORITHM = _PREFIX + "SCOPE_RUN_GRAD_ALGORITHM" + _SUFFIX;

	public static final String READ_BATCH_JOB_TYPE = _PREFIX + "SCOPE_READ_GRAD_BATCH_JOB_CODE_DATA" + _SUFFIX;
	public static final String DELETE_BATCH_JOB_TYPE = _PREFIX + "SCOPE_DELETE_GRAD_BATCH_JOB_CODE_DATA" + _SUFFIX;
	public static final String UPDATE_BATCH_JOB_TYPE = _PREFIX + "SCOPE_UPDATE_GRAD_BATCH_JOB_CODE_DATA" + _SUFFIX;
	public static final String CREATE_BATCH_JOB_TYPE = _PREFIX + "SCOPE_CREATE_GRAD_BATCH_JOB_CODE_DATA" + _SUFFIX;
}
