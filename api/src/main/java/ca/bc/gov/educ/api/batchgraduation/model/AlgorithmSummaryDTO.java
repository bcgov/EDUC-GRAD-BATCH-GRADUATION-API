package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@JsonSerialize
public class AlgorithmSummaryDTO {

  private String tableName;
  private Long batchId;
  private long readCount = 0L;
  private long processedCount = 0L;

  List<UUID> successfulStudentIDs = new ArrayList<>();
  private Map<UUID,ProcessError> errors = new HashMap<>();
  private List<GraduationStudentRecord> globalList = new ArrayList<>();
  private String exception;

  // stats
  private Map<String, Long> programCountMap = new HashMap<>() {{
    put("2018-EN", 0L);
    put("2018-PF", 0L);
    put("2004-EN", 0L);
    put("2004-PF", 0L);
    put("1996-EN", 0L);
    put("1996-PF", 0L);
    put("1986-EN", 0L);
    put("1950", 0L);
    put("NOPROG", 0L);
    put("SCCP", 0L);
  }};

  private Map<String, SchoolReportRequest> mapDist = new HashMap<>();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String accessToken;

  public void increment(String programCode) {
    programCountMap.computeIfPresent(programCode,(key, val) -> val + 1);
  }
  public void updateError(UUID studentID,ProcessError newError) {
    errors.computeIfPresent(studentID,(key, val) -> newError);
  }
}
