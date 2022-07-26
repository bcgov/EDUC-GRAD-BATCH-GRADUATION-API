package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.BatchProcessing;
import ca.bc.gov.educ.api.batchgraduation.model.ScheduledJobs;
import ca.bc.gov.educ.api.batchgraduation.model.Task;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
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

    @Autowired TaskSchedulingService taskSchedulingService;
    @Autowired TaskDefinition taskDefinition;
    @Autowired GradDashboardService gradDashboardService;


    @PostMapping(EducGradBatchGraduationApiConstants.SCHEDULE_JOBS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Schedule Jobs", description = "Schedule Jobs", tags = { "Schedule" })
    public void scheduleATask(@RequestBody Task task) {
        taskDefinition.setTask(task);
        taskSchedulingService.scheduleATask(ThreadLocalStateUtil.getCurrentUser(),task.getJobName() , taskDefinition, task.getCronExpression());
    }

    @DeleteMapping(EducGradBatchGraduationApiConstants.REMOVE_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Schedule Jobs", description = "Schedule Jobs", tags = { "Schedule" })
    public void removeJob(@PathVariable String jobId) {
        String[] jK =   jobId.split("_");
        taskSchedulingService.removeScheduledTask(Integer.parseInt(jK[0]),jK[1],jK[2]);
    }

    @GetMapping(EducGradBatchGraduationApiConstants.LIST_JOBS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Schedule Jobs", description = "Schedule Jobs", tags = { "Schedule" })
    public ResponseEntity<List<ScheduledJobs>> listJobs() {
        List<ScheduledJobs> res = taskSchedulingService.listScheduledJobs();
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

    @PutMapping(EducGradBatchGraduationApiConstants.UPDATE_ENABLED)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Toggle Scheduled job availability", description = "Toggle Scheduled job availability", tags = { "Schedule" })
    public ResponseEntity<BatchProcessing> processingList(@PathVariable UUID processingId) {
        BatchProcessing res = gradDashboardService.toggleProcess(processingId);
        if(res == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
