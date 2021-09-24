package ca.bc.gov.educ.api.batchgraduation.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.model.BatchInfoDetails;
import ca.bc.gov.educ.api.batchgraduation.model.GradDashboard;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchInfoDetailsRepository;
import ca.bc.gov.educ.api.batchgraduation.transformer.BatchInfoDetailsTransformer;

@Service
public class GradDashboardService extends GradService {

	private final BatchInfoDetailsRepository  batchInfoDetailsRepository;
	private final BatchInfoDetailsTransformer batchInfoDetailsTransformer;

    public GradDashboardService(BatchInfoDetailsRepository batchInfoDetailsRepository,BatchInfoDetailsTransformer batchInfoDetailsTransformer) {
    	this.batchInfoDetailsRepository = batchInfoDetailsRepository;
    	this.batchInfoDetailsTransformer = batchInfoDetailsTransformer;
	}

    @Transactional
	public GradDashboard getDashboardInfo() {
    	start();
    	GradDashboard gradDash = new GradDashboard();
    	List<BatchInfoDetails> infoDetailsList= batchInfoDetailsTransformer.transformToDTO(batchInfoDetailsRepository.findAll());
    	Collections.sort(infoDetailsList, Comparator.comparing(BatchInfoDetails::getJobExecutionId).reversed());  
    	if(!infoDetailsList.isEmpty()) {
    		BatchInfoDetails info = infoDetailsList.get(0);
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
