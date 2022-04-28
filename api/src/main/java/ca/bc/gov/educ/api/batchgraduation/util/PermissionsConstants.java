package ca.bc.gov.educ.api.batchgraduation.util;

public interface PermissionsConstants {
	String _PREFIX = "hasAuthority('";
	String _SUFFIX = "')";

	String LOAD_STUDENT_IDS = _PREFIX + "SCOPE_LOAD_STUDENT_IDS" + _SUFFIX;
	String LOAD_DASHBOARD = _PREFIX + "SCOPE_LOAD_BATCH_DASHBOARD" + _SUFFIX;
	String RUN_GRAD_ALGORITHM = _PREFIX + "SCOPE_RUN_GRAD_ALGORITHM" + _SUFFIX;
}
