package ca.bc.gov.educ.api.batchgraduation.model;

import ca.bc.gov.educ.api.batchgraduation.util.GradLocalDateTimeDeserializer;
import ca.bc.gov.educ.api.batchgraduation.util.GradLocalDateTimeSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseModel {
	private String createUser;
	@JsonSerialize(using = GradLocalDateTimeSerializer.class)
	@JsonDeserialize(using = GradLocalDateTimeDeserializer.class)
	private LocalDateTime createDate;
	private String updateUser;
	@JsonSerialize(using = GradLocalDateTimeSerializer.class)
	@JsonDeserialize(using = GradLocalDateTimeDeserializer.class)
	private LocalDateTime updateDate;

}
