package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class EdwSnapshotSchoolSummaryDTO extends BaseSummaryDTO {
    private Integer gradYear;
    private List<SnapshotResponse> globalList = new ArrayList<>();
}
