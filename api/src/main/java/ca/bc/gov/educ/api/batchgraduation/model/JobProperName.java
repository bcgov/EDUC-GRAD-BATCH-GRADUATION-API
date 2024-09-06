package ca.bc.gov.educ.api.batchgraduation.model;

public enum JobProperName {
	SGBJ ("Special Graduation Batch Job"),
	STBJ ("Special Achievement Batch Job"),
    YDBJ ("Year-End Distribution Batch Job"),
    MDBJ ("Monthly Distribution Batch Job"),
    SDBJ ("Supplemental Distribution Batch Job"),
    NDBJ("Non-Grad Distribution Batch Job"),
	URDBJ ("User Req Distribution Batch Job"),
    URPDBJ("PSI Distribution Batch Job"),
    BDBJ ("Blank Distribution Batch Job"),
    RCBJ ("Regenerate Certificates Batch Job"),
    DSRBJ ("Delete Student Report Batch Job"),
    ASBJ("Archive Students Batch Job"),
    ASRBJ("Archive School Reports Batch Job"),
    SRRBJ("User Request School Report Regeneration")
    ;

    private final String value;

    JobProperName(String value){
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
