package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class DistributionSummaryDTO extends BaseDistributionSummaryDTO {

  private List<StudentCredentialDistribution> globalList = new ArrayList<>();
  private List<School> schools = new ArrayList<>();

  @Override
  public void initializeCredentialCountMap() {
    credentialCountMap.put("YED4", 0L);
    credentialCountMap.put("YED2", 0L);
    credentialCountMap.put("YEDR", 0L);
    credentialCountMap.put("YEDB", 0L);
  }
}
