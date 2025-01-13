package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class SchoolReport extends BaseModel {

    private UUID id;
    private String report;
    private String reportTypeCode;
    private String reportTypeLabel;
    private UUID schoolOfRecordId;
    private String schoolOfRecordName;
    private String schoolCategory;

}