package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchProcessingEntity;
import ca.bc.gov.educ.api.batchgraduation.model.BatchPipelineStatus;
import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessingSchedule;
import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessingScheduleUpdateRequest;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchProcessingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
public class BatchProcessingScheduleService {

    private static final String REGALG = "REGALG";
    private static final DateTimeFormatter START_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final BatchProcessingRepository batchProcessingRepository;
    private final SystemBatchSchedulingService systemBatchSchedulingService;
    private final GradBatchHistoryService gradBatchHistoryService;

    public BatchProcessingScheduleService(BatchProcessingRepository batchProcessingRepository,
                                         SystemBatchSchedulingService systemBatchSchedulingService,
                                         GradBatchHistoryService gradBatchHistoryService) {
        this.batchProcessingRepository = batchProcessingRepository;
        this.systemBatchSchedulingService = systemBatchSchedulingService;
        this.gradBatchHistoryService = gradBatchHistoryService;
    }

    @Transactional(readOnly = true)
    public BatchProcessingSchedule getBatchProcessingSchedule(String jobType) {
        BatchProcessingEntity entity = batchProcessingRepository.findByJobType(normalizeJobType(jobType))
                .orElseThrow(() -> new IllegalArgumentException("No schedule found for job type " + jobType));
        return toScheduleResponse(entity, resolveZoneId(null));
    }

    @Transactional
    public BatchProcessingSchedule updateBatchProcessingSchedule(String jobType, BatchProcessingScheduleUpdateRequest request) {
        String normalizedJobType = normalizeJobType(jobType);
        if (!REGALG.equals(normalizedJobType)) {
            throw new IllegalArgumentException("Only REGALG supports schedule updates.");
        }
        validateNoActiveBatchPipelineRun();
        BatchProcessingEntity entity = batchProcessingRepository.findByJobType(normalizedJobType)
                .orElseThrow(() -> new IllegalArgumentException("No schedule found for job type " + jobType));

        ZoneId zoneId = resolveZoneId(request.getTimeZone());
        LocalDateTime scheduledDateTime = parseScheduledDateTime(request.getScheduledDateTime());
        validateScheduledDateTime(scheduledDateTime, zoneId);

        entity.setCronExpression(toDailyCron(scheduledDateTime.toLocalTime()));
        BatchProcessingEntity updated = batchProcessingRepository.save(entity);
        systemBatchSchedulingService.refreshScheduledJob(normalizedJobType);
        return toScheduleResponse(updated, zoneId);
    }

    private String normalizeJobType(String jobType) {
        if (jobType == null || jobType.isBlank()) {
            throw new IllegalArgumentException("Job type is required.");
        }
        return jobType.toUpperCase(Locale.ROOT);
    }

    private ZoneId resolveZoneId(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Invalid time zone: " + timeZone);
        }
    }

    private LocalDateTime parseScheduledDateTime(String scheduledDateTime) {
        if (scheduledDateTime == null || scheduledDateTime.isBlank()) {
            throw new IllegalArgumentException("scheduledDateTime is required.");
        }
        try {
            return LocalDateTime.parse(scheduledDateTime);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("scheduledDateTime must be ISO-8601 formatted, for example 2026-07-23T22:00:00.");
        }
    }

    private void validateScheduledDateTime(LocalDateTime scheduledDateTime, ZoneId zoneId) {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime requested = scheduledDateTime.atZone(zoneId);
        if (!requested.toLocalDate().equals(now.toLocalDate())) {
            throw new IllegalArgumentException("Scheduled date must be today, " + now.toLocalDate() + ".");
        }
        if (!requested.isAfter(now)) {
            throw new IllegalArgumentException("Scheduled time must be later than the current time.");
        }
    }

    private void validateNoActiveBatchPipelineRun() {
        BatchPipelineStatus pipelineStatus = gradBatchHistoryService.getBatchPipelineStatus();
        if (pipelineStatus != null && pipelineStatus.isRunning()) {
            throw new IllegalArgumentException("Cannot update the REGALG schedule while a REGALG or TVRRUN batch is running.");
        }
    }

    private String toDailyCron(LocalTime localTime) {
        return String.format("0 %d %d * * *", localTime.getMinute(), localTime.getHour());
    }

    private BatchProcessingSchedule toScheduleResponse(BatchProcessingEntity entity, ZoneId zoneId) {
        LocalTime startTime = extractStartTime(entity.getCronExpression());
        BatchProcessingSchedule response = new BatchProcessingSchedule();
        response.setJobType(entity.getJobType());
        response.setEnabled(entity.getEnabled());
        response.setCronExpression(entity.getCronExpression());
        response.setStartTime(startTime.format(START_TIME_FORMAT));
        response.setScheduledDateTime(LocalDate.now(zoneId).atTime(startTime).toString());
        response.setTimeZone(zoneId.getId());
        return response;
    }

    private LocalTime extractStartTime(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("Cron expression is not configured.");
        }
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Cron expression must contain 6 fields.");
        }
        try {
            int minute = Integer.parseInt(parts[1]);
            int hour = Integer.parseInt(parts[2]);
            return LocalTime.of(hour, minute);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Cron expression does not represent a fixed daily start time.");
        }
    }
}
