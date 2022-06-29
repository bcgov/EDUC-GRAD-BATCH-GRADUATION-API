package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.UUID;

@Data
public class SchoolReportDistribution {

	private UUID id;
	private String reportTypeCode;
	private String schoolOfRecord;
}