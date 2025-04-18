package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class GraduationStudentRecord extends BaseModel {

    private String studentGradData;
    private String studentProjectedGradData;
    private String pen;
    private String program;
    private String programName;
    private String programCompletionDate;
    private String gpa;
    private String honoursStanding;
    private String recalculateGradStatus;
    private UUID schoolOfRecordId;
    private String schoolName;
    private String studentGrade;	
    private String studentStatus;
    private String studentStatusName;
    private UUID studentID;
    private UUID schoolAtGradId;
    private String schoolAtGradName;
    private String legalFirstName;
    private String legalMiddleNames;
    private String legalLastName;
				
}
