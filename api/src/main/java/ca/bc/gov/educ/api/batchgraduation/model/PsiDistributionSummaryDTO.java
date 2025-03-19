package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;


@Data
@NoArgsConstructor
public class PsiDistributionSummaryDTO extends BaseDistributionSummaryDTO {

  private List<PsiCredentialDistribution> globalList = new ArrayList<>();
  private Map<String, DistributionPrintRequest> mapDist = new TreeMap<>();

  @Override
  public void initializeCredentialCountMap() {
    credentialCountMap.put("YED4", 0L);
    credentialCountMap.put("YED2", 0L);
    credentialCountMap.put("YEDR", 0L);
    credentialCountMap.put("YEDB", 0L);
  }

}
