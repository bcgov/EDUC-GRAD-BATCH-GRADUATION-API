package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.List;

@Data
public class SchoolReportRequest {

	private List<GraduationStudentRecord> studentList;
}
