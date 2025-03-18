package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.*;

@Data
public abstract class BaseDistributionSummaryDTO<T extends Comparable<T>> extends BaseSummaryDTO {
    private List<ProcessError> errors = new ArrayList<>();
    private String credentialType;

    // stats
    protected Map<String, Long> credentialCountMap = new HashMap<>();

    private Map<T, DistributionPrintRequest> mapDist = new TreeMap<>();

    public abstract void initializeCredentialCountMap();

    public void increment(String programCode) {
        credentialCountMap.computeIfPresent(programCode,(key, val) -> val + 1);
    }

    public void increment(String programCode, int count) {
        credentialCountMap.computeIfPresent(programCode,(key, val) -> val + count);
    }

    public void setCredentialCounter(String programCode, int count) {
        credentialCountMap.computeIfPresent(programCode,(key, val) -> (long) (count));
    }
}
