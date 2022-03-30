package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class ErrorBoard {

	private String pen;
	private String legalFirstName;
	private String legalMiddleNames;
	private String legalLastName;
	private String error;
	
}
