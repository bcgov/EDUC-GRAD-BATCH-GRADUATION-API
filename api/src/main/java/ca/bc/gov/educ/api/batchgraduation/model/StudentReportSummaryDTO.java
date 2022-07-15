package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class StudentReportSummaryDTO {

  private Long batchId;
  private long readCount = 0L;
  private long processedCount = 0L;

  private List<ProcessError> errors = new ArrayList<>();
  private List<SchoolStudentCredentialDistribution> globalList = new ArrayList<>();
  private String exception;
  private String credentialType;

  // stats
  private Map<String, Long> credentialCountMap = new HashMap<>();

  public void initializeCredentialCountMap() {
    credentialCountMap.put("ACHV", 0L);
    credentialCountMap.put("BC2018-PUB", 0L);
    credentialCountMap.put("YU2018-PUB", 0L);
    credentialCountMap.put("BC1950-PUB", 0L);
    credentialCountMap.put("YU1950-PUB", 0L);
    credentialCountMap.put("BC2018-IND", 0L);
    credentialCountMap.put("BC1950-IND", 0L);
    credentialCountMap.put("BC2018-OFF", 0L);
    credentialCountMap.put("BC2018-PF", 0L);
    credentialCountMap.put("SCCP-EN", 0L);
    credentialCountMap.put("SCCP-FR", 0L);
    credentialCountMap.put("BC2004-PUB", 0L);
    credentialCountMap.put("YU2004-PUB", 0L);
    credentialCountMap.put("BC1996-PUB", 0L);
    credentialCountMap.put("YU1996-PUB", 0L);
    credentialCountMap.put("BC1986-PUB", 0L);
    credentialCountMap.put("YU1986-PUB", 0L);
    credentialCountMap.put("BC2004-IND", 0L);
    credentialCountMap.put("BC1996-IND", 0L);
    credentialCountMap.put("BC1986-IND", 0L);
    credentialCountMap.put("NOPROG", 0L);
  }

  private Map<String, DistributionPrintRequest> mapDist = new HashMap<>();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String accessToken;

  public void increment(String programCode) {
    credentialCountMap.computeIfPresent(programCode,(key, val) -> val + 1);
  }
}
