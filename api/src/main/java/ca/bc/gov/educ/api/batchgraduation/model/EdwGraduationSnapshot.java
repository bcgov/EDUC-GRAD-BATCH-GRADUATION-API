package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdwGraduationSnapshot {
    private UUID studentID;

    private Integer gradYear;
    private String pen;
    private String graduationFlag;
    private String honoursStanding;
    private BigDecimal gpa;
    private String graduatedDate;

    private String nonGradReason1;
    private String nonGradReason2;
    private String nonGradReason3;
    private String nonGradReason4;
    private String nonGradReason5;
    private String nonGradReason6;
    private String nonGradReason7;
    private String nonGradReason8;
    private String nonGradReason9;
    private String nonGradReason10;
    private String nonGradReason11;
    private String nonGradReason12;

    private String runDate;
    private String sessionDate;
    private String schoolOfRecord;
    private UUID schoolOfRecordId;
}
