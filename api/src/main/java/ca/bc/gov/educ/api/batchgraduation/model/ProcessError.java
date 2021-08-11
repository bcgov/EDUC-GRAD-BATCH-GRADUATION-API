package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class ProcessError {
  private String pen;
  private String reason;
}
