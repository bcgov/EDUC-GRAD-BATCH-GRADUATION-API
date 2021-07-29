package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GradCourseRestriction {
    private UUID courseRestrictionId;
    private String mainCourse;
    private String mainCourseLevel;
    private String restrictedCourse;
    private String restrictedCourseLevel;
    private String restrictionStartDate;
    private String restrictionEndDate;
}
