package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@JsonSerialize
public class AlgorithmSummaryDTO extends BaseSummaryDTO {

//  List<UUID> successfulStudentIDs = new ArrayList<>();
  private Map<UUID,ProcessError> errors = new HashMap<>();

  public void updateError(UUID studentID,String errMsg, String errorDesc) {
    ProcessError obj = errors.get(studentID);
    if(obj == null) {
      obj = new ProcessError();
    }
    obj.setReason(errMsg);
    obj.setDetail(errorDesc);
    errors.put(studentID,obj);
  }
}
