package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class DistributionSummaryDTO extends BaseDistributionSummaryDTO {

    private List<StudentCredentialDistribution> globalList = new ArrayList<>();
    private List<School> schools = new ArrayList<>();
    private StudentSearchRequest studentSearchRequest;

    @Override
    public void initializeCredentialCountMap() {
        credentialCountMap.put("YED4", 0L);
        credentialCountMap.put("YED2", 0L);
        credentialCountMap.put("YEDR", 0L);
        credentialCountMap.put("YEDB", 0L);
    }

    public void recalculateCredentialCounts() {
        credentialCountMap.clear();
        for(StudentCredentialDistribution scd: globalList) {
            String paperType = scd.getPaperType();
            if(StringUtils.isNotBlank(paperType)) {
                increment(paperType);
            }
        }
        setReadCount(globalList.size());
        setProcessedCount(0);
    }
}
