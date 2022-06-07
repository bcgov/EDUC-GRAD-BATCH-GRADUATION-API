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
public class BlankDistributionSummaryDTO {

  private Long batchId;
  private long readCount = 0L;
  private long processedCount = 0L;

  private List<ProcessError> errors = new ArrayList<>();
  private List<BlankCredentialDistribution> globalList = new ArrayList<>();
  private String exception;

  // stats
  private Map<String, Long> credentialCountMap = new HashMap<>() {{
    put("F", 0L);
    put("E", 0L);
    put("EI", 0L);
    put("S", 0L);
    put("A", 0L);
    put("AI", 0L);
    put("SC", 0L);
    put("O", 0L);
    put("SCF", 0L);
  }};

  private Map<String, DistributionPrintRequest> mapDist = new HashMap<>();

  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String accessToken;

  public void increment(String programCode) {
    credentialCountMap.computeIfPresent(programCode,(key, val) -> val + 1);
  }
}
