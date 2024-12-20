package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@EqualsAndHashCode(of = {"studentID", "credentialTypeCode", "paperType", "documentStatusCode"})
public class StudentCredentialDistribution implements Serializable {

	private UUID id;
	private String credentialTypeCode;
	private UUID studentID;
	private String paperType;
	private String schoolOfRecord;
	private UUID schoolId;
	private String documentStatusCode;

	private String pen;
	private String legalFirstName;
	private String legalMiddleNames;
	private String legalLastName;
	private String programCompletionDate;
	private String lastUpdateDate;
	private String honoursStanding;
	private String program;
	private String studentGrade;
	private List<GradRequirement> nonGradReasons;

	@JsonIgnore
	private String schoolAtGrad;
	@JsonIgnore
	private String schoolOfRecordOrigin;
	@JsonIgnore
	private UUID districtId;

}
