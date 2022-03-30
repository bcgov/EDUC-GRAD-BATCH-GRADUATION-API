package ca.bc.gov.educ.api.batchgraduation.service;

import java.util.*;
import java.util.stream.Collectors;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.ErrorBoard;
import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmErrorHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.model.BatchGradAlgorithmJobHistory;
import ca.bc.gov.educ.api.batchgraduation.model.GradDashboard;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.transformer.BatchGradAlgorithmJobHistoryTransformer;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradDashboardService extends GradService {

	private final BatchGradAlgorithmJobHistoryRepository  batchGradAlgorithmJobHistoryRepository;
	private final BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository;
	private final BatchGradAlgorithmJobHistoryTransformer batchGradAlgorithmJobHistoryTransformer;
	private final RestUtils restUtils;

    public GradDashboardService(BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository,BatchGradAlgorithmJobHistoryTransformer batchGradAlgorithmJobHistoryTransformer,BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository,RestUtils restUtils) {
    	this.batchGradAlgorithmJobHistoryRepository = batchGradAlgorithmJobHistoryRepository;
    	this.batchGradAlgorithmJobHistoryTransformer = batchGradAlgorithmJobHistoryTransformer;
		this.batchGradAlgorithmErrorHistoryRepository = batchGradAlgorithmErrorHistoryRepository;
		this.restUtils = restUtils;
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

    public List<ErrorBoard> getErrorInfo(Long batchId, Integer pageNumber, Integer pageSize,String accessToken) {
		Pageable paging = PageRequest.of(pageNumber, pageSize);
		Page<BatchGradAlgorithmErrorHistoryEntity> pagedDate = batchGradAlgorithmErrorHistoryRepository.findByJobExecutionId(batchId,paging);
		List<BatchGradAlgorithmErrorHistoryEntity> list = pagedDate.getContent();
		List<UUID> studentIds = list.stream().map(BatchGradAlgorithmErrorHistoryEntity::getStudentID).collect(Collectors.toList());
		List<ErrorBoard> eList = new ArrayList<>();
		if(!studentIds.isEmpty()) {
			List<GraduationStudentRecord> studentList = restUtils.getStudentData(studentIds, accessToken);

			for (GraduationStudentRecord gRec : studentList) {
				ErrorBoard eD = new ErrorBoard();
				BatchGradAlgorithmErrorHistoryEntity ent = batchGradAlgorithmErrorHistoryRepository.findByStudentID(gRec.getStudentID());
				eD.setError(ent.getError());
				eD.setLegalFirstName(gRec.getLegalFirstName());
				eD.setLegalLastName(gRec.getLegalLastName());
				eD.setLegalMiddleNames(gRec.getLegalMiddleNames());
				eD.setPen(gRec.getPen());
				eList.add(eD);
			}
		}
		return eList;
    }
}
