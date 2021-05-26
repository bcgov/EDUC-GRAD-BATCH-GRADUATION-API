package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GradCourseRestrictions {
    private String mainCourse;
    private String mainCourseLevel;
    private String restrictedCourse;
    private String restrictedCourseLevel;
    private String restrictionStartDate;
    private String restrictionEndDate;
}
