package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRegenerationRequest extends StudentSearchRequest {
    private String runMode; // "Y" or "N"

    public boolean runForAll () {
        return (getPens() == null || getPens().isEmpty()) &&
                (getDistrictIds() == null || getDistrictIds().isEmpty()) &&
                (getSchoolIds() == null || getSchoolIds().isEmpty()) &&
                (getSchoolCategoryCodes() == null || getSchoolCategoryCodes().isEmpty()) &&
                (getStudentIDs() == null || getStudentIDs().isEmpty());
    }
}
