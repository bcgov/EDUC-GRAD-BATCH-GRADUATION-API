package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
public class EdwSnapshotSummaryDTO extends BaseSummaryDTO {
    private Integer gradYear;
    private Map<String, Long> countMap = new HashMap<>();

    private Map<String,ProcessSnapshotError> errors = new HashMap<>();

    public void updateError(String pen, String schoolOfRecord, String errMsg, String errorDesc) {
        ProcessSnapshotError obj = errors.get(pen);
        if(obj == null) {
            obj = new ProcessSnapshotError();
        }
        obj.setSchoolOfRecord(schoolOfRecord);
        obj.setReason(errMsg);
        obj.setDetail(errorDesc);
        errors.put(pen,obj);
    }
}
