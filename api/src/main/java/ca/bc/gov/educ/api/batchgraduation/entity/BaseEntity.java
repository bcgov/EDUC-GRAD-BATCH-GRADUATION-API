package ca.bc.gov.educ.api.batchgraduation.entity;

import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import jakarta.persistence.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
public class BaseEntity {
	@Column(name = "CREATE_USER")
    private String createUser;
	
	@Column(name = "CREATE_DATE", columnDefinition="datetime",nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	@DateTimeFormat(pattern = "yyyy-mm-dd hh:mm:ss")
    private LocalDateTime createDate;
	
	
	@Column(name = "UPDATE_USER", nullable = false)
    private String updateUser;
	
	@Column(name = "UPDATE_DATE", columnDefinition="datetime")
	@Temporal(TemporalType.TIMESTAMP)
	@DateTimeFormat(pattern = "yyyy-mm-dd hh:mm:ss")
	private LocalDateTime updateDate;
	
	@PrePersist
	protected void onCreate() {
		if (StringUtils.isBlank(createUser)) {
			this.createUser = ThreadLocalStateUtil.getCurrentUser();
			if (StringUtils.isBlank(createUser)) {
				this.createUser = EducGradBatchGraduationApiConstants.DEFAULT_CREATED_BY;
			}
		}		
		if (StringUtils.isBlank(updateUser)) {
			this.updateUser = ThreadLocalStateUtil.getCurrentUser();
			if (StringUtils.isBlank(updateUser)) {
				this.updateUser = EducGradBatchGraduationApiConstants.DEFAULT_UPDATED_BY;
			}
		}		
		this.createDate = LocalDateTime.now();
		this.updateDate = LocalDateTime.now();

	}

	@PreUpdate
	protected void onPersist() {
		this.updateDate = LocalDateTime.now();
		if (StringUtils.isBlank(updateUser)) {
			this.updateUser = ThreadLocalStateUtil.getCurrentUser();
			if (StringUtils.isBlank(updateUser)) {
				this.updateUser = EducGradBatchGraduationApiConstants.DEFAULT_UPDATED_BY;
			}
		}
		if (StringUtils.isBlank(createUser)) {
			this.createUser = ThreadLocalStateUtil.getCurrentUser();
			if (StringUtils.isBlank(createUser)) {
				this.createUser = EducGradBatchGraduationApiConstants.DEFAULT_CREATED_BY;
			}
		}
		if (this.createDate == null) {
			this.createDate = LocalDateTime.now();
		}
	}
}
