package ca.bc.gov.educ.api.batchgraduation.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.model.BatchGradAlgorithmJobHistory;
import ca.bc.gov.educ.api.batchgraduation.model.GradDashboard;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.transformer.BatchGradAlgorithmJobHistoryTransformer;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradDashboardService extends GradService {

	private final BatchGradAlgorithmJobHistoryRepository  batchGradAlgorithmJobHistoryRepository;
	private final BatchGradAlgorithmJobHistoryTransformer batchGradAlgorithmJobHistoryTransformer;

    public GradDashboardService(BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository,BatchGradAlgorithmJobHistoryTransformer batchGradAlgorithmJobHistoryTransformer) {
    	this.batchGradAlgorithmJobHistoryRepository = batchGradAlgorithmJobHistoryRepository;
    	this.batchGradAlgorithmJobHistoryTransformer = batchGradAlgorithmJobHistoryTransformer;
	}

    @Transactional(readOnly = true)
	public GradDashboard getDashboardInfo() {
    	start();
    	GradDashboard gradDash = new GradDashboard();
    	List<BatchGradAlgorithmJobHistory> infoDetailsList= batchGradAlgorithmJobHistoryTransformer.transformToDTO(batchGradAlgorithmJobHistoryRepository.findAll());
    	Collections.sort(infoDetailsList, Comparator.comparing(BatchGradAlgorithmJobHistory::getStartTime).reversed());  
    	if(!infoDetailsList.isEmpty()) {
    		BatchGradAlgorithmJobHistory info = infoDetailsList.get(0);
    		gradDash.setLastActualStudentsProcessed(info.getActualStudentsProcessed());
    		gradDash.setLastExpectedStudentsProcessed(info.getExpectedStudentsProcessed());
    		gradDash.setLastFailedStudentsProcessed(info.getFailedStudentsProcessed());
    		gradDash.setLastJobendTime(info.getEndTime());
    		gradDash.setLastJobExecutionId(info.getJobExecutionId());
    		gradDash.setLastJobstartTime(info.getStartTime());
    		gradDash.setLastStatus(info.getStatus());
    		gradDash.setTotalBatchRuns(infoDetailsList.size());
    		gradDash.setBatchInfoList(infoDetailsList);
    	
    	}
    	end();
		return gradDash;
    }
}
