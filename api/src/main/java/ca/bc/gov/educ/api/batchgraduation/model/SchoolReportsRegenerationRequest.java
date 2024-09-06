package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
public class SchoolReportsRegenerationRequest extends StudentSearchRequest {
    private String runMode; // "Y" or "N"

    public boolean runForAll () {
        return (getSchoolOfRecords() == null || getSchoolOfRecords().isEmpty()) &&
                (getSchoolCategoryCodes() == null || getSchoolCategoryCodes().isEmpty());
    }
}
