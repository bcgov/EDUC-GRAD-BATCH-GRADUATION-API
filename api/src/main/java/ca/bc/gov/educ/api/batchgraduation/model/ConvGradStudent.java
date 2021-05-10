package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Date;

@Data
@AllArgsConstructor
public class ConvGradStudent {
  private String pen;
  private String program;
  private Date programCompletionDate;
  private String gpa;
  private String honoursStanding;
  private String recalculateGradStatus;
  private String studentGradData;
  private String schoolOfRecord;
  private String schoolAtGrad;
  private String studentGrade;
  private String studentStatus;

  // extra
  private String graduationRequestYear;
}
