package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConversionSummaryDTO {
  // GRAD_STUDENT
  private long gradStudentReadCount = 0;
  private long gradStudentAddedCount = 0;
  private long gradStudentUpdatedCount = 0;

  private List<ConversionError> errors = new ArrayList<>();

  // Code Tables
  // GRAD_COURSE_RESTRICTIONS
  private long courseRestrictionsCount = 0;

}
