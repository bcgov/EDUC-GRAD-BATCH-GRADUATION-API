package ca.bc.gov.educ.api.batchgraduation.model;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class ExceptionMessage {

	private String exceptionName;
	private String exceptionDetails;
}
