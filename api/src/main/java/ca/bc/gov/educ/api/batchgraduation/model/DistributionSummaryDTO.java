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
public class DistributionSummaryDTO {

  private Long batchId;
  private long readCount = 0L;
  private long processedCount = 0L;

  private List<ProcessError> errors = new ArrayList<>();
  private List<StudentCredentialDistribution> globalList = new ArrayList<>();
  private String exception;
  private String credentialType;

  // stats
  private Map<String, Long> credentialCountMap = new HashMap<>() {{
    put("YED4", 0L);
    put("YED2", 0L);
    put("YEDR", 0L);
    put("YEDB", 0L);
  }};

  private Map<String, DistributionPrintRequest> mapDist = new HashMap<>();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String accessToken;

  public void increment(String programCode) {
    Long count = credentialCountMap.get(programCode);
    if (count != null) {
      count++;
      credentialCountMap.put(programCode, count);
    }
  }
}
