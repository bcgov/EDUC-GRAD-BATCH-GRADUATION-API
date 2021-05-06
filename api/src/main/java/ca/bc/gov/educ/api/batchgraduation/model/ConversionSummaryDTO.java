package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConversionSummaryDTO {

  private String tableName;

  private long readCount = 0;
  private long processedCount = 0;

  private long addedCount = 0;
  private long updatedCount = 0;

  private List<ConversionError> errors = new ArrayList<>();
  private String exception;

}
