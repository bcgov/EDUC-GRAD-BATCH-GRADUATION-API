package ca.bc.gov.educ.api.batchgraduation.model;

public enum TaskSelection {
	SGBJ ("SpecialGraduationBatchJob"),
	STBJ ("SpecialTvrRunBatchJob"),
    YDBJ ("YearlyDistributionBatchJob"),
    MDBJ ("DistributionBatchJob"),
	URDBJ ("UserReqDistributionBatchJob"),
    URPDBJ("psiDistributionBatchJob"),
    BDBJ ("blankDistributionBatchJob");

    private final String value;

    TaskSelection(String value){
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
