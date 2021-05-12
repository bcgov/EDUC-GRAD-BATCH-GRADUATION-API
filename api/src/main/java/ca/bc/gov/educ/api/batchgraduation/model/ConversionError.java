package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

@Data
public class ConversionError {
  private String pen;
  private String reason;
}
