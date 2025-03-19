package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
public class BlankDistributionSummaryDTO extends BaseDistributionSummaryDTO {

  private List<BlankCredentialDistribution> globalList = new ArrayList<>();
  private Map<UUID, DistributionPrintRequest> mapDist = new TreeMap<>();

  @Override
  public void initializeCredentialCountMap() {
    credentialCountMap.put("F", 0L);
    credentialCountMap.put("E", 0L);
    credentialCountMap.put("EI", 0L);
    credentialCountMap.put("S", 0L);
    credentialCountMap.put("A", 0L);
    credentialCountMap.put("AI", 0L);
    credentialCountMap.put("SC", 0L);
    credentialCountMap.put("O", 0L);
    credentialCountMap.put("SCF", 0L);
  }
}
