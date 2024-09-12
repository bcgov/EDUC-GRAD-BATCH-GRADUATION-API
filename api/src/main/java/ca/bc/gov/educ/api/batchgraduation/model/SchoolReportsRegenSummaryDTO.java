package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SchoolReportsRegenSummaryDTO extends BaseSummaryDTO {

    private List<ProcessError> errors = new ArrayList<>();
    private List<School> globalList = new ArrayList<>();
    private List<School> schools = new ArrayList<>();
    private StudentSearchRequest studentSearchRequest;

}
