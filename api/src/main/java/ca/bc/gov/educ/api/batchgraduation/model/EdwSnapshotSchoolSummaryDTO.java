package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class EdwSnapshotSchoolSummaryDTO extends BaseSummaryDTO {
    private Integer gradYear;
    private String option; // A = All current students,  L = All grad students + nongrad students(12, AD)
    private List<SnapshotResponse> globalList = new ArrayList<>();
}
