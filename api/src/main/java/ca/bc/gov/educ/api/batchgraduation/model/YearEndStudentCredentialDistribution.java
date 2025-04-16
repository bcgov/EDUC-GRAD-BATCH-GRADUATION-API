package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(of = {"studentID", "credentialTypeCode", "paperType", "documentStatusCode"})
public class YearEndStudentCredentialDistribution extends StudentCredentialDistribution {

    private UUID schoolOfRecordId;
    private UUID schoolAtGradId;
    private UUID districtId;
    private UUID districtAtGradId;

    private String reportingSchoolTypeCode;

    private String transcriptTypeCode;
    private String certificateTypeCode;

}
