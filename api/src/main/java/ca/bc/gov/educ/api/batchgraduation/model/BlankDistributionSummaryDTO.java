package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class BlankDistributionSummaryDTO extends BaseDistributionSummaryDTO {

  private List<BlankCredentialDistribution> globalList = new ArrayList<>();

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
