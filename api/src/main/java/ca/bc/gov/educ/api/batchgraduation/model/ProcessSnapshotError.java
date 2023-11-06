package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessSnapshotError {
  private String studentID;
  private String schoolOfRecord;
  private String reason;
  private String detail;
}
