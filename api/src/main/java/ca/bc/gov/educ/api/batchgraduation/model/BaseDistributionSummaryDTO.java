package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public abstract class BaseDistributionSummaryDTO extends BaseSummaryDTO {
    private List<ProcessError> errors = new ArrayList<>();
    private String credentialType;

    // stats
    protected Map<String, Long> credentialCountMap = new HashMap<>();

    private Map<String, DistributionPrintRequest> mapDist = new HashMap<>();

    abstract void initializeCredentialCountMap();

    public void increment(String programCode) {
        credentialCountMap.computeIfPresent(programCode,(key, val) -> val + 1);
    }
}
