package ca.bc.gov.educ.api.batchgraduation.util;

public interface PermissionsConstants {
	String _PREFIX = "#oauth2.hasAnyScope('";
	String _SUFFIX = "')";

	String LOAD_STUDENT_IDS = _PREFIX + "LOAD_STUDENT_IDS" + _SUFFIX;
	String LOAD_DASHBOARD = _PREFIX + "LOAD_BATCH_DASHBOARD" + _SUFFIX;
	String RUN_GRAD_ALGORITHM = _PREFIX + "RUN_GRAD_ALGORITHM" + _SUFFIX;
}
