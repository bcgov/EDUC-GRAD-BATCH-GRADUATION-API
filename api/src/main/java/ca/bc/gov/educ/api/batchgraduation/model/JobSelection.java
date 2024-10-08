package ca.bc.gov.educ.api.batchgraduation.model;

public enum JobSelection {
	SGBJ ("REGALG"),
	STBJ ("TVRRUN"),
    YDBJ ("DISTRUN_YE"),
    MDBJ ("DISTRUN"),
    SDBJ("DISTRUN_SUPP"),
    NDBJ("NONGRADRUN"),
	URDBJ ("DISTRUNUSER"),
    URPDBJ ("PSIRUN"),
    BDBJ ("DISTRUNUSER"),
    RCBJ ("CERT_REGEN"),
    DSRBJ ("TVR_DELETE"),
    ASBJ("ARC_STUDENTS"),
    ASRBJ("ARC_SCH_REPORTS"),
    SRRBJ("SCHL_RPT_REGEN");

    private final String value;

    JobSelection(String value){
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
