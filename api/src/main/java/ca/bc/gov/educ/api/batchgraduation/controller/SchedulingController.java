package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessing;
import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessingSchedule;
import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessingScheduleUpdateRequest;
import ca.bc.gov.educ.api.batchgraduation.model.BatchPipelineStatus;
import ca.bc.gov.educ.api.batchgraduation.model.Task;
import ca.bc.gov.educ.api.batchgraduation.model.UserScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.BatchProcessingScheduleService;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import ca.bc.gov.educ.api.batchgraduation.service.SystemBatchSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.service.TaskDefinition;
import ca.bc.gov.educ.api.batchgraduation.service.TaskSchedulingService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.PermissionsConstants;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
@OpenAPIDefinition(info = @Info(title = "API for Manual Triggering of batch process.", description = "This API is for Manual Triggering of batch process.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"LOAD_STUDENT_IDS","LOAD_BATCH_DASHBOARD","RUN_GRAD_ALGORITHM"})})
public class SchedulingController {

    TaskSchedulingService taskSchedulingService;
    TaskDefinition taskDefinition;
    GradDashboardService gradDashboardService;
    GradBatchHistoryService gradBatchHistoryService;
    BatchProcessingScheduleService batchProcessingScheduleService;
    SystemBatchSchedulingService systemBatchSchedulingService;

    @Autowired
    public SchedulingController(TaskSchedulingService taskSchedulingService, TaskDefinition taskDefinition, GradDashboardService gradDashboardService, GradBatchHistoryService gradBatchHistoryService, BatchProcessingScheduleService batchProcessingScheduleService, SystemBatchSchedulingService systemBatchSchedulingService) {
        this.taskSchedulingService = taskSchedulingService;
        this.taskDefinition = taskDefinition;
        this.gradDashboardService = gradDashboardService;
        this.gradBatchHistoryService = gradBatchHistoryService;
        this.batchProcessingScheduleService = batchProcessingScheduleService;
        this.systemBatchSchedulingService = systemBatchSchedulingService;
    }

    @PostMapping(EducGradBatchGraduationApiConstants.SCHEDULE_JOBS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Schedule Jobs", description = "Schedule Jobs", tags = { "Schedule" })
    public void scheduleATask(@RequestBody Task task, @RequestParam(required = false) String batchJobTypeCode) {
        if(task.isDeliveredToUser()) {
            task.setProperUserName(ThreadLocalStateUtil.getProperName());
        }
        taskSchedulingService.saveUserScheduledJobs(task, batchJobTypeCode);
        taskDefinition.setTask(task);
        taskSchedulingService.scheduleATask(task.getJobIdReference(),taskDefinition, task.getCronExpression());
    }

    @DeleteMapping(EducGradBatchGraduationApiConstants.REMOVE_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Schedule Jobs", description = "Schedule Jobs", tags = { "Schedule" })
    public void removeJob(@PathVariable String jobId) {
        taskSchedulingService.removeScheduledTask(UUID.fromString(jobId));
    }

    @GetMapping(EducGradBatchGraduationApiConstants.LIST_JOBS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Schedule Jobs", description = "Schedule Jobs", tags = { "Schedule" })
    public ResponseEntity<List<UserScheduledJobs>> listJobs() {
        List<UserScheduledJobs> res = taskSchedulingService.listScheduledJobs();
        if(res.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping(EducGradBatchGraduationApiConstants.PROCESSING_LIST)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Schedule Jobs", description = "Schedule Jobs", tags = { "Schedule" })
    public ResponseEntity<List<BatchProcessing>> processingList() {
        List<BatchProcessing> res = gradDashboardService.getProcessingList();
        if(res.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping(EducGradBatchGraduationApiConstants.PIPELINE_STATUS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Get REGALG/TVRRUN pipeline status", description = "Get REGALG/TVRRUN pipeline status", tags = { "Schedule" })
    public ResponseEntity<BatchPipelineStatus> getBatchPipelineStatus() {
        return new ResponseEntity<>(gradBatchHistoryService.getBatchPipelineStatus(), HttpStatus.OK);
    }

    @PutMapping(EducGradBatchGraduationApiConstants.UPDATE_ENABLED)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Toggle Scheduled job availability", description = "Toggle Scheduled job availability", tags = { "Schedule" })
    public ResponseEntity<BatchProcessing> toggleProcess(@PathVariable String jobType) {
        BatchProcessing res = gradDashboardService.toggleProcess(jobType);
        if(res == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        systemBatchSchedulingService.refreshScheduledJob(jobType);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping(EducGradBatchGraduationApiConstants.PROCESSING_SCHEDULE)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Get scheduled batch start time", description = "Get scheduled batch start time", tags = { "Schedule" })
    public ResponseEntity<BatchProcessingSchedule> getProcessingSchedule(@PathVariable String jobType) {
        return new ResponseEntity<>(batchProcessingScheduleService.getBatchProcessingSchedule(jobType), HttpStatus.OK);
    }

    @PutMapping(EducGradBatchGraduationApiConstants.PROCESSING_SCHEDULE)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Update scheduled batch start time", description = "Update scheduled batch start time", tags = { "Schedule" })
    public ResponseEntity<BatchProcessingSchedule> updateProcessingSchedule(@PathVariable String jobType,
                                                                           @RequestBody BatchProcessingScheduleUpdateRequest request) {
        return new ResponseEntity<>(batchProcessingScheduleService.updateBatchProcessingSchedule(jobType, request), HttpStatus.OK);
    }
}
