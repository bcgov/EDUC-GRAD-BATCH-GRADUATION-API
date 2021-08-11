package ca.bc.gov.educ.api.batchgraduation.model;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class StudentOptionalProgram extends BaseModel{

    private UUID id;
    private String pen;
    private UUID optionalProgramID;
    private String studentSpecialProgramData;
    private String specialProgramCompletionDate;
    private String specialProgramName;
    private String specialProgramCode;
    private String programCode;
    private UUID studentID;
}
