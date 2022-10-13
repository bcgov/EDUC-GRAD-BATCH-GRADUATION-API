package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.UUID;

@Data
public class BatchGraduationStudentRecord {

	private UUID studentID;
	private String program;
	private String programCompletionDate;
	private String schoolOfRecord;

}
