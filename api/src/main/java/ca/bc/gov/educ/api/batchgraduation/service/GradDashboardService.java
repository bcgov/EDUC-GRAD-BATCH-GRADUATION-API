package ca.bc.gov.educ.api.batchgraduation.service;

import java.util.*;
import java.util.stream.Collectors;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmErrorHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchJobExecutionEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmErrorHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchJobExecutionRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchProcessingRepository;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.transformer.BatchProcessingTransformer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.transformer.BatchGradAlgorithmJobHistoryTransformer;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradDashboardService extends GradService {

	private final BatchGradAlgorithmJobHistoryRepository  batchGradAlgorithmJobHistoryRepository;
	private final BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository;
	private final BatchGradAlgorithmJobHistoryTransformer batchGradAlgorithmJobHistoryTransformer;
	private final BatchJobExecutionRepository batchJobExecutionRepository;
	private final BatchProcessingTransformer batchProcessingTransformer;
	private final BatchProcessingRepository batchProcessingRepository;
	private final RestUtils restUtils;

    public GradDashboardService(BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository,BatchGradAlgorithmJobHistoryTransformer batchGradAlgorithmJobHistoryTransformer,BatchGradAlgorithmErrorHistoryRepository batchGradAlgorithmErrorHistoryRepository,RestUtils restUtils,BatchJobExecutionRepository batchJobExecutionRepository,BatchProcessingRepository batchProcessingRepository,BatchProcessingTransformer batchProcessingTransformer) {
    	this.batchGradAlgorithmJobHistoryRepository = batchGradAlgorithmJobHistoryRepository;
    	this.batchGradAlgorithmJobHistoryTransformer = batchGradAlgorithmJobHistoryTransformer;
		this.batchProcessingTransformer = batchProcessingTransformer;
		this.batchGradAlgorithmErrorHistoryRepository = batchGradAlgorithmErrorHistoryRepository;
		this.batchJobExecutionRepository = batchJobExecutionRepository;
		this.batchProcessingRepository = batchProcessingRepository;
		this.restUtils = restUtils;
	}

    @Transactional(readOnly = true)
	public GradDashboard getDashboardInfo() {
    	start();
    	GradDashboard gradDash = new GradDashboard();
    	List<BatchGradAlgorithmJobHistory> infoDetailsList= batchGradAlgorithmJobHistoryTransformer.transformToDTO(batchGradAlgorithmJobHistoryRepository.findAll());
    	infoDetailsList.sort(Comparator.comparing(BatchGradAlgorithmJobHistory::getStartTime).reversed());
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

	@Transactional(readOnly = true)
    public ErrorDashBoard getErrorInfo(Long batchId, Integer pageNumber, Integer pageSize,String accessToken) {
		ErrorDashBoard edb = new ErrorDashBoard();
		Pageable paging = PageRequest.of(pageNumber, pageSize);
		Page<BatchGradAlgorithmErrorHistoryEntity> pagedDate = batchGradAlgorithmErrorHistoryRepository.findByJobExecutionId(batchId,paging);
		List<BatchGradAlgorithmErrorHistoryEntity> list = pagedDate.getContent();
		List<UUID> studentIds = list.stream().map(BatchGradAlgorithmErrorHistoryEntity::getStudentID).collect(Collectors.toList());
		List<ErrorBoard> eList = new ArrayList<>();
		if(!studentIds.isEmpty()) {
			List<GraduationStudentRecord> studentList = restUtils.getStudentData(studentIds, accessToken);

			for (GraduationStudentRecord gRec : studentList) {
				ErrorBoard eD = new ErrorBoard();
				BatchGradAlgorithmErrorHistoryEntity ent = batchGradAlgorithmErrorHistoryRepository.findByStudentIDAndJobExecutionId(gRec.getStudentID(),batchId);
				eD.setError(ent.getError());
				eD.setLegalFirstName(gRec.getLegalFirstName());
				eD.setLegalLastName(gRec.getLegalLastName());
				eD.setLegalMiddleNames(gRec.getLegalMiddleNames());
				eD.setPen(gRec.getPen());
				eList.add(eD);
			}
		}
		edb.setErrorList(eList);
		edb.setPageable(pagedDate.getPageable());
		edb.setNumber(pagedDate.getNumber());
		edb.setSize(pagedDate.getSize());
		edb.setSort(pagedDate.getSort());
		edb.setTotalElements(pagedDate.getTotalElements());
		edb.setTotalPages(pagedDate.getTotalPages());
		edb.setNumberOfElements(pagedDate.getNumberOfElements());
		return edb;
    }

	@Transactional(readOnly = true)
    public SummaryDashBoard getBatchSummary(Integer pageNumber, Integer pageSize) {
		Pageable paging = PageRequest.of(pageNumber, pageSize);
		Page<BatchJobExecutionEntity> pagedData = batchJobExecutionRepository.findAllByOrderByCreateTimeDesc(paging);
		SummaryDashBoard edb = new SummaryDashBoard();
		edb.setBatchJobList(pagedData.getContent());
		edb.setPageable(pagedData.getPageable());
		edb.setNumber(pagedData.getNumber());
		edb.setSize(pagedData.getSize());
		edb.setSort(pagedData.getSort());
		edb.setTotalElements(pagedData.getTotalElements());
		edb.setTotalPages(pagedData.getTotalPages());
		edb.setNumberOfElements(pagedData.getNumberOfElements());
		return edb;
    }

	@Transactional(readOnly = true)
    public List<BatchProcessing> getProcessingList() {
		return batchProcessingTransformer.transformToDTO(batchProcessingRepository.findAll());
    }

	@Transactional
	public BatchProcessing toggleProcess(String jobType) {
		Optional<BatchProcessingEntity> opt = batchProcessingRepository.findByJobType(jobType);
		if(opt.isPresent()) {
			BatchProcessingEntity ent = opt.get();
			ent.setUpdateUser(null);
			ent.setUpdateDate(null);
			ent.setEnabled(ent.getEnabled().equalsIgnoreCase("Y")?"N":"Y");
			return batchProcessingTransformer.transformToDTO(batchProcessingRepository.save(ent));
		}
		return null;
	}

	@Transactional(readOnly = true)
	public Optional<BatchProcessingEntity> findBatchProcessing(String jobType) {
		return batchProcessingRepository.findByJobType(jobType);
	}
}
