package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.model.BatchPipelineStatus;
import ca.bc.gov.educ.api.batchgraduation.model.BatchPipelineRunStatus;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmStudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GradBatchHistoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GradBatchHistoryService.class);
    private static final List<String> PIPELINE_JOB_TYPES = List.of("REGALG", "TVRRUN");
    private static final Set<String> ACTIVE_STATUSES = Set.of(
            BatchStatusEnum.STARTING.name(),
            BatchStatusEnum.STARTED.name(),
            BatchStatusEnum.STOPPING.name()
    );
    private static final Duration ACTIVE_WINDOW = Duration.ofHours(12);
    private static final Duration HEARTBEAT_THROTTLE = Duration.ofSeconds(5);
    private static final Duration WARNING_THRESHOLD = Duration.ofMinutes(10);
    private static final Duration INSPECT_THRESHOLD = Duration.ofMinutes(20);
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final String HEALTH_OK = "ok";
    private static final String HEALTH_WARNING = "warning";
    private static final String HEALTH_INSPECT = "please_inspect";

    @Autowired
    private BatchGradAlgorithmJobHistoryRepository batchGradAlgorithmJobHistoryRepository;

    @Autowired
    private BatchGradAlgorithmStudentRepository batchGradAlgorithmStudentRepository;

    private final Map<Long, LocalDateTime> heartbeatThrottleMap = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public BatchGradAlgorithmJobHistoryEntity getGradAlgorithmJobHistory(Long batchId) {
        Optional<BatchGradAlgorithmJobHistoryEntity> optional = batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(batchId);
        return optional.orElse(null);
    }

    @Transactional
    public BatchGradAlgorithmJobHistoryEntity saveGradAlgorithmJobHistory(BatchGradAlgorithmJobHistoryEntity ent) {
        Optional<BatchGradAlgorithmJobHistoryEntity> optional = batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(ent.getJobExecutionId());
        if (optional.isPresent()) {
            LOGGER.info("Updating BatchGradAlgorithmJobHistoryEntity for Id :{} Status: {}, EndTime: {} ", ent.getJobExecutionId(), ent.getStatus(), ent.getEndTime());
            BatchGradAlgorithmJobHistoryEntity current = optional.get();
            current.setStatus(ent.getStatus());
            if(BatchStatusEnum.COMPLETED.name().equalsIgnoreCase(ent.getStatus()) || BatchStatusEnum.FAILED.name().equalsIgnoreCase(ent.getStatus()) || BatchStatusEnum.STOPPED.name().equalsIgnoreCase(ent.getStatus())) {
                current.setEndTime(ent.getEndTime());
            }
            current.setExpectedStudentsProcessed(ent.getExpectedStudentsProcessed());
            current.setActualStudentsProcessed(ent.getActualStudentsProcessed());
            current.setFailedStudentsProcessed(ent.getFailedStudentsProcessed());
            current.setLocalDownload(ent.getLocalDownload());
            current.setJobParameters(ent.getJobParameters());
            if (ent.getLastHeartbeatTime() != null) {
                current.setLastHeartbeatTime(ent.getLastHeartbeatTime());
            } else if (current.getLastHeartbeatTime() == null) {
                current.setLastHeartbeatTime(LocalDateTime.now());
            }
            return batchGradAlgorithmJobHistoryRepository.save(current);
        } else {
            if (ent.getLastHeartbeatTime() == null) {
                ent.setLastHeartbeatTime(LocalDateTime.now());
            }
            // create
            return batchGradAlgorithmJobHistoryRepository.save(ent);
        }
    }

    @Transactional
    public void touchHeartbeat(Long batchId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastHeartbeat = heartbeatThrottleMap.get(batchId);
        if (lastHeartbeat != null && lastHeartbeat.plus(HEARTBEAT_THROTTLE).isAfter(now)) {
            return;
        }
        BatchGradAlgorithmJobHistoryEntity history = batchGradAlgorithmJobHistoryRepository.findByJobExecutionId(batchId).orElse(null);
        if (history != null) {
            history.setLastHeartbeatTime(now);
            batchGradAlgorithmJobHistoryRepository.save(history);
            heartbeatThrottleMap.put(batchId, now);
        }
    }

    @Transactional(readOnly = true)
    public BatchPipelineStatus getBatchPipelineStatus() {
        BatchPipelineStatus response = new BatchPipelineStatus();
        response.setRunning(false);
        LocalDateTime startTimeCutoff = LocalDateTime.now().minus(ACTIVE_WINDOW);
        List<BatchGradAlgorithmJobHistoryEntity> recentRuns =
                batchGradAlgorithmJobHistoryRepository.findRecentByJobTypesAndStartTimeAfter(PIPELINE_JOB_TYPES, startTimeCutoff);

        for (BatchGradAlgorithmJobHistoryEntity run : recentRuns) {
            if (!ACTIVE_STATUSES.contains(run.getStatus())) {
                continue;
            }
            String healthStatus = determineHealthStatus(run.getLastHeartbeatTime(), run.getStartTime());
            BatchPipelineRunStatus runStatus = toRunStatus(run, healthStatus);
            if (HEALTH_INSPECT.equals(healthStatus)) {
                response.getStaleRuns().add(runStatus);
                continue;
            }
            response.getActiveRuns().add(runStatus);
        }

        if (!response.getActiveRuns().isEmpty()) {
            response.setRunning(true);
        }
        if (!response.getStaleRuns().isEmpty()) {
            response.setMessage("A stale batch was found. Please inspect batch history.");
        }
        return response;
    }

    private BatchPipelineRunStatus toRunStatus(BatchGradAlgorithmJobHistoryEntity history, String healthStatus) {
        BatchPipelineRunStatus runStatus = new BatchPipelineRunStatus();
        runStatus.setJobExecutionId(history.getJobExecutionId());
        runStatus.setJobType(history.getJobType());
        runStatus.setStatus(history.getStatus());
        runStatus.setStartTime(history.getStartTime());
        runStatus.setLastHeartbeat(history.getLastHeartbeatTime());
        runStatus.setHealthStatus(healthStatus);
        return runStatus;
    }

    private String determineHealthStatus(LocalDateTime lastHeartbeat, LocalDateTime startTime) {
        LocalDateTime effectiveHeartbeat = lastHeartbeat != null ? lastHeartbeat : startTime;
        if (effectiveHeartbeat == null) {
            return HEALTH_INSPECT;
        }
        Duration staleness = Duration.between(
                effectiveHeartbeat.atZone(SYSTEM_ZONE),
                ZonedDateTime.now(SYSTEM_ZONE)
        );
        if (staleness.compareTo(INSPECT_THRESHOLD) > 0) {
            return HEALTH_INSPECT;
        }
        if (staleness.compareTo(WARNING_THRESHOLD) > 0) {
            return HEALTH_WARNING;
        }
        return HEALTH_OK;
    }

    @Transactional
    public void saveGradAlgorithmStudents(List<BatchGradAlgorithmStudentEntity> entList) {
        batchGradAlgorithmStudentRepository.saveAll(entList);
    }

    @Transactional
    public BatchGradAlgorithmStudentEntity saveGradAlgorithmStudent(BatchGradAlgorithmStudentEntity entity) {
        return batchGradAlgorithmStudentRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<BatchGradAlgorithmStudentEntity> getErroredStudents(Long batchId) {  // STARTED or FAILED
        return batchGradAlgorithmStudentRepository.findByJobExecutionIdAndStatusIn(batchId, Arrays.asList(BatchStatusEnum.STARTED.name(), BatchStatusEnum.FAILED.name()));
    }

    @Transactional(readOnly = true)
    public List<BatchGradAlgorithmStudentEntity> getAllStudents(Long batchId) {
        return batchGradAlgorithmStudentRepository.findByJobExecutionId(batchId);
    }

    @Transactional(readOnly = true)
    public BatchGradAlgorithmStudentEntity getBatchGradAlgorithmStudent(Long batchId, UUID studentID) {
        Optional<BatchGradAlgorithmStudentEntity> optional = batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentID, batchId);
        return optional.orElse(null);
    }

    @Transactional
    public void saveBatchAlgorithmStudent(Long batchId, UUID studentID, String program, UUID schoolId) {
        Optional<BatchGradAlgorithmStudentEntity> optional = batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentID, batchId);
        if (optional.isPresent()) {
            BatchGradAlgorithmStudentEntity currentEntity = optional.get();
            currentEntity.setProgram(program);
            currentEntity.setSchoolOfRecordId(schoolId);
            batchGradAlgorithmStudentRepository.save(currentEntity);
        } else {
            BatchGradAlgorithmStudentEntity entity = new BatchGradAlgorithmStudentEntity();
            entity.setJobExecutionId(batchId);
            entity.setStudentID(studentID);
            entity.setProgram(program);
            entity.setSchoolOfRecordId(schoolId);
            entity.setStatus(BatchStatusEnum.STARTED.name());
            batchGradAlgorithmStudentRepository.save(entity);
        }
    }

    @Transactional
    public void updateBatchStatusForStudent(Long batchId, UUID studentID, BatchStatusEnum batchStatus, String errorMessage) {
        Optional<BatchGradAlgorithmStudentEntity> optional = batchGradAlgorithmStudentRepository.findByStudentIDAndJobExecutionId(studentID, batchId);
        if (optional.isPresent()) {
            BatchGradAlgorithmStudentEntity currentEntity = optional.get();
            if (batchStatus == BatchStatusEnum.FAILED) {
                currentEntity.setError(errorMessage);
            }
            currentEntity.setStatus(batchStatus.name());
            batchGradAlgorithmStudentRepository.save(currentEntity);
        }
    }

    @Transactional(readOnly = true)
    public long getCountForErroredStudent(Long batchId) {
        return batchGradAlgorithmStudentRepository.countAllByJobExecutionIdAndStatusIn(batchId, Arrays.asList(BatchStatusEnum.FAILED.name(), BatchStatusEnum.STARTED.name()));
    }

    @Transactional(readOnly = true)
    public long getCountForReadStudent(Long batchId) {
        return batchGradAlgorithmStudentRepository.countAllByJobExecutionId(batchId);
    }

    @Transactional(readOnly = true)
    public long getCountForProcessedStudent(Long batchId) {
        return batchGradAlgorithmStudentRepository.countAllByJobExecutionIdAndStatus(batchId, BatchStatusEnum.COMPLETED.name());
    }

    @Transactional(readOnly = true)
    public List<UUID> getSchoolListForReport(Long batchId) {
        return batchGradAlgorithmStudentRepository.getSchoolList(batchId);
    }

    @Transactional
    public void copyAllStudentsIntoNewBatch(Long newBatchId, Long fromBatchId, String username) {
        batchGradAlgorithmStudentRepository.copyAllGradAlgorithmStudents(
                newBatchId, fromBatchId,
                username,
                new Date(System.currentTimeMillis())
        );
    }

    @Transactional
    public void copyErroredStudentsIntoNewBatch(Long newBatchId, Long fromBatchId, String username) {
        batchGradAlgorithmStudentRepository.copyGradAlgorithmErroredStudents(
                newBatchId, fromBatchId,
                username,
                new Date(System.currentTimeMillis())
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> getGraduationProgramCountsForBatchRunSummary(Long batchId) {
        Map<String, Integer> response = new HashMap<>();
        List<Object[]> results = batchGradAlgorithmStudentRepository.getGraduationProgramCounts(batchId);
        if (results != null && !results.isEmpty()) {
            results.forEach(field -> {
                String program = (String) field[0];
                BigDecimal count = (BigDecimal) field[1];
                response.put(program, count.intValue());
            });
        }
        return response;
    }

}
