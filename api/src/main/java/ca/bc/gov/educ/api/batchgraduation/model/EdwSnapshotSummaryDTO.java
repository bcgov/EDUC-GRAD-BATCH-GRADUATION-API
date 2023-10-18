package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
public class EdwSnapshotSummaryDTO extends AlgorithmSummaryDTO {
    private Integer gradYear;
    private Set<String> schools = new HashSet<>();
    private Map<String, Long> countMap = new HashMap<>();

}
