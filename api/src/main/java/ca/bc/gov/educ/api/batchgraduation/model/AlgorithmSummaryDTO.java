package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@JsonSerialize
public class AlgorithmSummaryDTO extends BaseSummaryDTO {

  List<UUID> successfulStudentIDs = new ArrayList<>();
  private Map<UUID,ProcessError> errors = new HashMap<>();
  private Set<String> schoolList = new HashSet<>();

  // stats
  private Map<String, Long> programCountMap = new HashMap<>();

  public void increment(String programCode) {
    programCountMap.computeIfPresent(programCode,(key, val) -> val + 1);
  }
  public void updateError(UUID studentID,String errMsg, String errorDesc) {
    ProcessError obj = errors.get(studentID);
    if(obj == null) {
      obj = new ProcessError();
    }
    obj.setReason(errMsg);
    obj.setDetail(errorDesc);
    errors.put(studentID,obj);
  }

  public void initializeProgramCountMap() {
    programCountMap.put("2018-EN", 0L);
    programCountMap.put("2018-PF", 0L);
    programCountMap.put("2004-EN", 0L);
    programCountMap.put("2004-PF", 0L);
    programCountMap.put("1996-EN", 0L);
    programCountMap.put("1996-PF", 0L);
    programCountMap.put("1986-EN", 0L);
    programCountMap.put("1950", 0L);
    programCountMap.put("NOPROG", 0L);
    programCountMap.put("SCCP", 0L);
  }
}
