package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.*;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.repository.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.transformer.BatchGradAlgorithmJobHistoryTransformer;
import ca.bc.gov.educ.api.batchgraduation.transformer.BatchProcessingTransformer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GradDashboardService extends GradService {

	private static final String JOB_STATUS_STARTED = BatchStatusEnum.STARTED.name();

	private final BatchGradAlgorithmJobHistoryRepository  batchGradAlgorithmJobHistoryRepository;
	private final BatchGradAlgorithmStudentRepository batchGradAlgorithmStudentRepository;
	private final BatchGradAlgorithmJobHistoryTransformer batchGradAlgorithmJobHistoryTransformer;
	private final BatchJobExecutionRepository batchJobExecutionRepository;
	private final BatchStepExecutionRepository batchStepExecutionRepository;
	private final BatchProcessingTransformer batchProcessingTransformer;
	private final BatchProcessingRepository batchProcessingRepository;
	private final RestUtils restUtils;

    public GradDashboardService(BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository,
								BatchGradAlgorithmJobHistoryTransformer batchGradAlgorithmJobHistoryTransformer,
								RestUtils restUtils,
								BatchJobExecutionRepository batchJobExecutionRepository,
								BatchStepExecutionRepository batchStepExecutionRepository,
								BatchProcessingRepository batchProcessingRepository,BatchProcessingTransformer batchProcessingTransformer,
								BatchGradAlgorithmStudentRepository batchGradAlgorithmStudentRepository) {
		this.batchGradAlgorithmJobHistoryRepository = batchGradAlgorithmJobHistoryRepository;
		this.batchGradAlgorithmJobHistoryTransformer = batchGradAlgorithmJobHistoryTransformer;
		this.batchProcessingTransformer = batchProcessingTransformer;
		this.batchGradAlgorithmStudentRepository = batchGradAlgorithmStudentRepository;
		this.batchJobExecutionRepository = batchJobExecutionRepository;
		this.batchStepExecutionRepository = batchStepExecutionRepository;
		this.batchProcessingRepository = batchProcessingRepository;
		this.restUtils = restUtils;
	}

    @Transactional(readOnly = true)
	public GradDashboard getDashboardInfo() {
		start();
		GradDashboard gradDash = new GradDashboard();
		List<BatchGradAlgorithmJobHistory> infoDetailsList= batchGradAlgorithmJobHistoryTransformer.transformToDTO(batchGradAlgorithmJobHistoryRepository.findAll());
		infoDetailsList = infoDetailsList.stream().map(this::handleDeadJob).map(this::handleFrozenJob).collect(Collectors.toList());
		infoDetailsList.sort(Comparator.comparing(BatchGradAlgorithmJobHistory::getStartTime).reversed());
		if(!infoDetailsList.isEmpty()) {
			BatchGradAlgorithmJobHistory info = infoDetailsList.get(0);
			gradDash.setLastActualStudentsProcessed(info.getActualStudentsProcessed());
			gradDash.setLastExpectedStudentsProcessed(info.getExpectedStudentsProcessed());
			gradDash.setLastFailedStudentsProcessed(info.getFailedStudentsProcessed());
			gradDash.setLastJobendTime(ca.bc.gov.educ.api.batchgraduation.util.DateUtils.toDate(info.getEndTime()));
			gradDash.setLastJobExecutionId(info.getJobExecutionId());
			gradDash.setLastJobstartTime(ca.bc.gov.educ.api.batchgraduation.util.DateUtils.toDate(info.getStartTime()));
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
		Page<BatchGradAlgorithmStudentEntity> pagedDate = batchGradAlgorithmStudentRepository.findByJobExecutionIdAndStatusIn(batchId, Arrays.asList(BatchStatusEnum.STARTED.toString(), BatchStatusEnum.FAILED.toString()), paging);
		List<BatchGradAlgorithmStudentEntity> list = pagedDate.getContent();
		List<UUID> studentIds = list.stream().map(BatchGradAlgorithmStudentEntity::getStudentID).toList();
		List<ErrorBoard> eList = new ArrayList<>();
		if(!studentIds.isEmpty()) {
			List<GraduationStudentRecord> studentList = restUtils.getStudentData(studentIds, accessToken);

			for (GraduationStudentRecord gRec : studentList) {
				ErrorBoard eD = new ErrorBoard();
				Optional<BatchGradAlgorithmStudentEntity> optional = batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(gRec.getStudentID(),batchId);
				if (optional.isPresent()) {
					BatchGradAlgorithmStudentEntity ent = optional.get();
					eD.setError(ent.getError());
					eD.setLegalFirstName(gRec.getLegalFirstName());
					eD.setLegalLastName(gRec.getLegalLastName());
					eD.setLegalMiddleNames(gRec.getLegalMiddleNames());
					eD.setPen(gRec.getPen());
					eList.add(eD);
				}
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

	/**
	 * If any batch jobs have "STARTED" status more than 3 days(72 hours), then treat it as FAILED job
	 */
	@Transactional
	public BatchGradAlgorithmJobHistory handleDeadJob(BatchGradAlgorithmJobHistory batchJobHistory) {
		if (JOB_STATUS_STARTED.equalsIgnoreCase(batchJobHistory.getStatus()) && batchJobHistory.getEndTime() == null) {
			LocalDateTime now = LocalDateTime.now();
			Duration duration = Duration.between(batchJobHistory.getStartTime(), now);
			long hours = duration.getSeconds() / 3600;
			if (hours > 72) {
				updateBatchJobStatus(batchJobHistory, BatchStatusEnum.FAILED);
			}
		}

		return batchJobHistory;
	}

	@Transactional
	public BatchGradAlgorithmJobHistory handleFrozenJob(BatchGradAlgorithmJobHistory batchJobHistory) {
		if (JOB_STATUS_STARTED.equalsIgnoreCase(batchJobHistory.getStatus()) && batchJobHistory.getEndTime() == null) {
			Integer jobExecutionId = batchJobHistory.getJobExecutionId();
			if (isFrozen(jobExecutionId.longValue())) {
				updateBatchJobStatus(batchJobHistory, BatchStatusEnum.FAILED);
			}
		}
		return batchJobHistory;
	}

	/**
	 * If last updated time is frozen more than 5 hours for all of "STARTED" steps, then treat it as FAILED job
	 */
	private boolean isFrozen(Long jobExecutionId) {
		List<BatchStepExecutionEntity> steps = batchStepExecutionRepository.findByJobExecutionIdOrderByEndTimeDesc(jobExecutionId);
		LocalDateTime now = LocalDateTime.now();
		boolean frozenStepFound = false;
		for (BatchStepExecutionEntity step : steps) {
			if (step.getStepName().contains("partition") && JOB_STATUS_STARTED.equalsIgnoreCase(step.getStatus())) {
				Duration duration = Duration.between(step.getLastUpdated(), now);
				long hours = duration.getSeconds() / 3600;
				frozenStepFound = hours > 5;
			}
		}
		return frozenStepFound;
	}

	private void updateBatchJobStatus(BatchGradAlgorithmJobHistory batchJobHistory, BatchStatusEnum batchStatus) {
		BatchGradAlgorithmJobHistoryEntity entity = batchGradAlgorithmJobHistoryTransformer.transformToEntity(batchJobHistory);
		entity.setStatus(batchStatus.toString());
		batchGradAlgorithmJobHistoryRepository.save(entity);
		batchJobHistory.setStatus(BatchStatusEnum.FAILED.toString());
	}
}
