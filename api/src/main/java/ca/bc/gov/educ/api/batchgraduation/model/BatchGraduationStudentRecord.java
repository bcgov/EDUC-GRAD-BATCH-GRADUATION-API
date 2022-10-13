package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class BatchGraduationStudentRecord {

	private UUID studentID;
	private String program;
	private String programCompletionDate;
	private String schoolOfRecord;

}
