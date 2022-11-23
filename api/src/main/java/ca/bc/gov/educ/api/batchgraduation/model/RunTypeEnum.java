package ca.bc.gov.educ.api.batchgraduation.model;

public enum RunTypeEnum {
    NORMAL_JOB_PROCESS,
    RETRY_STEP_FOR_ERRORED_STUDENTS,
    RERUN_ALL_STUDENTS_FROM_PREVIOUS_JOB,
    RERUN_ERRORED_STUDENTS_FROM_PREVIOUS_JOB
}
