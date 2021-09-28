package ca.bc.gov.educ.api.batchgraduation.model;

import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class GradDashboard {

	private List<BatchGradAlgorithmJobHistory> batchInfoList;
	private Integer lastJobExecutionId; 
	private Date lastJobstartTime;
	private Date lastJobendTime;
	private String lastExpectedStudentsProcessed;
	private String lastActualStudentsProcessed;
	private String lastFailedStudentsProcessed;
	private String lastStatus;
	
	private Integer totalBatchRuns;
	
}
