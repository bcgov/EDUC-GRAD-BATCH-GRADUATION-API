package ca.bc.gov.educ.api.batchgraduation.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "CONV_GRAD_COURSE_RESTRICTIONS")
public class ConvCourseRestrictionsEntity extends BaseEntity  {
   
	@Id
	@Column(name = "ID", nullable = false)
    private UUID courseRestrictionId;

	@Column(name = "CRSE_MAIN", nullable = false)
    private String mainCourse;  
	
	@Column(name = "CRSE_MAIN_LVL", nullable = true)
    private String mainCourseLevel;
	
	@Column(name = "CRSE_RESTRICTED", nullable = false)
    private String restrictedCourse; 
	
	@Column(name = "CRSE_RESTRICTED_LVL", nullable = true)
    private String restrictedCourseLevel;   
	
	@Column(name = "RESTRICTION_START_DT", nullable = true)
    private Date restrictionStartDate; 
	
	@Column(name = "RESTRICTION_END_DT", nullable = true)
    private Date restrictionEndDate;

	@Column(name = "RESTRICTION_START_DT_STR", nullable = true)
	private String restrictionStartDateStr;

	@Column(name = "RESTRICTION_END_DT_STR", nullable = true)
	private String restrictionEndDateStr;

}
