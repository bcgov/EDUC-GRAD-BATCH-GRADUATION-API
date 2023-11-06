package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class SnapshotRequest {
    private Integer gradYear;
    private String option;  // A = All current students,  L = All grad students + nongrad students(12, AD)
    private List<String> schoolOfRecords;
}
