package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.exception.GradBusinessRuleException;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.processor.DistributionRunStatusUpdateProcessor;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import ca.bc.gov.educ.api.batchgraduation.util.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants.SEARCH_REQUEST;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
@OpenAPIDefinition(info = @Info(title = "API for Manual Triggering of batch process.", description = "This API is for Manual Triggering of batch process.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"LOAD_STUDENT_IDS","LOAD_BATCH_DASHBOARD","RUN_GRAD_ALGORITHM"})})
public class JobLauncherController {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";
    private static final String RERUN_TYPE = "reRunType";
    private static final String RUN_BY = "runBy";
    private static final String PREV_BATCH_ID = "previousBatchId";
    private static final String MANUAL = "MANUAL";
    private static final String TVRRUN = "TVRRUN";
    private static final String REGALG = "REGALG";
    private static final String CERT_REGEN = "CERT_REGEN";
    private static final String SCHL_RPT_REGEN = "SCHL_RPT_REGEN";
    private static final String EDW_SNAPSHOT = "EDW_SNAPSHOT";
    private static final String ARCHIVE_SCHOOL_REPORTS = "ARC_SCH_REPORTS";
    private static final String DELETE_STUDENT_REPORTS = "TVR_DELETE";
    private static final String ARCHIVE_STUDENTS = "ARC_STUDENTS";
    private static final String RERUN_ALL = "RERUN_ALL";
    private static final String RERUN_FAILED = "RERUN_FAILED";
    private static final String DISTRUN = "DISTRUN";
    private static final String DISTRUN_YE = "DISTRUN_YE";
    private static final String DISTRUN_SUPP = "DISTRUN_SUPP";
    private static final String NONGRADRUN = "NONGRADRUN";
    private static final String DISTRUNUSER = "DISTRUNUSER";
    private static final String PSIDISTRUN = "PSIRUN";
    private static final String CREDENTIALTYPE = "credentialType";
    private static final String TRANMISSION_TYPE = "transmissionType";
    private static final String DISDTO = "distributionSummaryDTO";
    private static final String LOCALDOWNLOAD = "LocalDownload";
    private static final String BEARER = "Bearer ";
    private static final String GRADUATION_BATCH_JOB = "GraduationBatchJob";
    private static final String TVR_BATCH_JOB = "tvrBatchJob";
    private static final String SPECIAL_GRADUATION_BATCH_JOB = "SpecialGraduationBatchJob";
    private static final String SPECIAL_TVR_RUN_BATCH_JOB = "SpecialTvrRunBatchJob";
    private static final String CERTIFICATE_REGENERATION_BATCH_JOB = "certRegenBatchJob";
    private static final String EDW_SNAPSHOT_BATCH_JOB = "edwSnapshotBatchJob";
    private static final String ARCHIVE_SCHOOL_REPORTS_BATCH_JOB = "archiveSchoolReportsBatchJob";
    private static final String DELETE_STUDENT_REPORTS_BATCH_JOB = "deleteStudentReportsBatchJob";
    private static final String ARCHIVE_STUDENTS_BATCH_JOB = "archiveStudentsBatchJob";
    private static final String REGENERATE_SCHOOL_REPORTS_BATCH_JOB = "schoolReportsRegenBatchJob";

    private final JobLauncher jobLauncher;
    private final JobLauncher asyncJobLauncher;
    private final JobRegistry jobRegistry;
    private final RestUtils restUtils;
    private final GradDashboardService gradDashboardService;
    private final GradBatchHistoryService gradBatchHistoryService;
    private final DistributionRunStatusUpdateProcessor distributionRunStatusUpdateProcessor;
    private final JsonTransformer jsonTransformer;
    private final GradSchoolOfRecordFilter gradSchoolOfRecordFilter;
    private final GradValidation gradValidation;

    @Autowired
    public JobLauncherController(
            JobLauncher jobLauncher,
            @Qualifier("asyncJobLauncher")
            JobLauncher asyncJobLauncher,
            JobRegistry jobRegistry,
            RestUtils restUtils,
            GradDashboardService gradDashboardService,
            GradBatchHistoryService gradBatchHistoryService,
            DistributionRunStatusUpdateProcessor distributionRunStatusUpdateProcessor,
            JsonTransformer jsonTransformer,
            GradSchoolOfRecordFilter gradSchoolOfRecordFilter,
            GradValidation gradValidation) {
        this.jobLauncher = jobLauncher;
        this.asyncJobLauncher = asyncJobLauncher;
        this.jobRegistry = jobRegistry;
        this.restUtils = restUtils;
        this.gradDashboardService = gradDashboardService;
        this.gradBatchHistoryService = gradBatchHistoryService;
        this.distributionRunStatusUpdateProcessor = distributionRunStatusUpdateProcessor;
        this.jsonTransformer = jsonTransformer;
        this.gradSchoolOfRecordFilter = gradSchoolOfRecordFilter;
        this.gradValidation = gradValidation;
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_REG_GRAD_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Manual Reg Grad Job", description = "Run Manual Reg Grad Job", tags = { "Reg Grad" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchRegGradJob() {
        logger.debug("launchRegGradJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, REGALG);
        response.setJobType(REGALG);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob(GRADUATION_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }

    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_TVR_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Manual TVR Job", description = "Run Manual TVR Job", tags = { "TVR" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchTvrRunJob() {
        logger.debug("launchTvrRunJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, TVRRUN);
        response.setJobType(TVRRUN);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob(TVR_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.LOAD_STUDENT_IDS)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    @Operation(summary = "Load Students to GRAD", description = "Load Students to GRAD", tags = { "Student" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<String> loadStudentIDs(@RequestBody List<LoadStudentData> loadStudentData) {
        logger.debug("Inside loadStudentIDs");
        Integer recordsAdded = restUtils.getStudentByPenFromStudentAPI(loadStudentData);
        if(recordsAdded != null)
            return ResponseEntity.ok("Record Added Successfully");
        return ResponseEntity.status(500).body("Student Record Could not be added");
    }

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_DASHBOARD)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    @Operation(summary = "Load all batch runs info", description = "Load all batch runs info", tags = { "Dashboard" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "204", description = "No Content")})
    public ResponseEntity<GradDashboard> loadDashboard() {
        logger.debug("Inside loadDashboard");
        GradDashboard dash = gradDashboardService.getDashboardInfo();
        if(dash != null) {
            return new ResponseEntity<>(dash,HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_ERRORS)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    @Operation(summary = "Load Error students in batch runs", description = "Load Error students in batch runs", tags = { "Dashboard" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "204", description = "No Content")})
    public ResponseEntity<ErrorDashBoard> loadError(@PathVariable Long batchId, @RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        logger.debug("Inside loadError");
        ErrorDashBoard dash = gradDashboardService.getErrorInfo(batchId,pageNumber,pageSize);
        if(dash == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(dash,HttpStatus.OK);
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized Regular Grad Runs", description = "Run specialized Regular Grad runs", tags = { "Reg Grad" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchRegGradSpecialJob(@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchRegGradSpecialJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, REGALG);
        response.setJobType(REGALG);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());
        validateInput(response, studentSearchRequest);
        if(response.getException() != null) {
            return ResponseEntity.status(400).body(response);
        }
        try {
            String studentSearchData = jsonTransformer.marshall(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            response.setJobParameters(studentSearchData);
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob(SPECIAL_GRADUATION_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private void validateInput(BatchJobResponse response, StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest.getStudentIDs().isEmpty() && studentSearchRequest.getPens().isEmpty() && studentSearchRequest.getDistrictIds().isEmpty() && studentSearchRequest.getSchoolCategoryCodes().isEmpty() && studentSearchRequest.getPrograms().isEmpty() && studentSearchRequest.getSchoolIds().isEmpty()) {
            response.setException("Please provide at least 1 parameter");
        }
        response.setException(null);
    }

    private DistributionSummaryDTO validateInputDisRun(StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest.getPens().isEmpty() && studentSearchRequest.getDistrictIds().isEmpty() && studentSearchRequest.getSchoolCategoryCodes().isEmpty() && studentSearchRequest.getPrograms().isEmpty() && studentSearchRequest.getSchoolIds().isEmpty()) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException("Please provide at least 1 parameter");
            return summaryDTO;
        }
        return null;
    }

    private void validateInputArchiveSchoolReports(StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest.getReportTypes().isEmpty()) {
            throw new GradBusinessRuleException("Please provide at least 1 report type parameter (GRADREG, NONGRADPRJ, NONGRADREG)");
        }
    }

    private void validateInputDeleteStudentReports(StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest == null) {
            throw new GradBusinessRuleException("Please provide not null student search request");
        }
        StringBuilder errors = new StringBuilder();
        if(studentSearchRequest.isEmpty() && StringUtils.isBlank(studentSearchRequest.getActivityCode())) {
            errors.append("Please provide school of records or set activityCode to ALL to delete all reports").append('\n');
        }
        if(studentSearchRequest.getReportTypes().isEmpty()) {
            errors.append("Please provide at least 1 report type code").append('\n');
        }
        String errorsAsString = errors.toString();
        if(StringUtils.isNotBlank(errorsAsString)) {
            throw new GradBusinessRuleException(errorsAsString);
        }
    }

    private void validateInputArchiveStudents(StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest == null) {
            throw new GradBusinessRuleException("Please provide not null student search request");
        }
        StringBuilder errors = new StringBuilder();
        if(studentSearchRequest.isEmpty() && StringUtils.isBlank(studentSearchRequest.getActivityCode())) {
            errors.append("Please provide school of records or set activityCode to ALL to archive all students").append('\n');
        }
        String errorsAsString = errors.toString();
        if(StringUtils.isNotBlank(errorsAsString)) {
            throw new GradBusinessRuleException(errorsAsString);
        }
    }

    private BlankDistributionSummaryDTO validateInputBlankDisRun(BlankCredentialRequest blankCredentialRequest) {
        if(blankCredentialRequest.getSchoolIds().isEmpty() || blankCredentialRequest.getCredentialTypeCode().isEmpty()) {
            BlankDistributionSummaryDTO summaryDTO = new BlankDistributionSummaryDTO();
            summaryDTO.setException("Please provide both parameters");
            return summaryDTO;
        }
        return null;
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_TVR_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized TVR Runs", description = "Run specialized TVR runs", tags = { "TVR" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchTvrRunSpecialJob(@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchTvrRunSpecialJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, TVRRUN);
        response.setJobType(TVRRUN);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());
        validateInput(response, studentSearchRequest);
        if(response.getException() != null) {
            return ResponseEntity.status(400).body(response);
        }
        try {
            String studentSearchData = jsonTransformer.marshall(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            response.setJobParameters(studentSearchData);
            JobExecution jobExecution =  asyncJobLauncher.run(jobRegistry.getJob(SPECIAL_TVR_RUN_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_RE_RUNS_ALL)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Re-Run REGALG or TVRRUN Job for all students from the given batchJobId", description = "Re-Run REGALG or TVRRUN Job for all students from the given batchJobId", tags = { "RE-RUN" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchRerunAll(@PathVariable Long batchId) {
        BatchJobResponse response = new BatchJobResponse();
        BatchGradAlgorithmJobHistoryEntity entity = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, entity.getTriggerBy());
        builder.addString(JOB_TYPE,  entity.getJobType());
        response.setJobType(entity.getJobType());
        response.setTriggerBy(entity.getTriggerBy());
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            boolean isSpecialRun = false;
            if (StringUtils.isNotBlank(entity.getJobParameters())) {
                builder.addString(SEARCH_REQUEST, entity.getJobParameters());
                response.setJobParameters(entity.getJobParameters());
                isSpecialRun = true;
            }
            builder.addString(RERUN_TYPE, RERUN_ALL);
            builder.addLong(PREV_BATCH_ID, batchId);
            String jobName;
            switch(entity.getJobType()) {
                case REGALG:
                    if (isSpecialRun) {
                        jobName = SPECIAL_GRADUATION_BATCH_JOB;

                    } else {
                        jobName = GRADUATION_BATCH_JOB;
                    }
                    break;
                case TVRRUN:
                    if (isSpecialRun) {
                        jobName = SPECIAL_TVR_RUN_BATCH_JOB;
                    } else {
                        jobName = TVR_BATCH_JOB;
                    }
                    break;
                default:
                    jobName = "Not Known";
                    throw new NoSuchJobException(jobName);
            }
            JobExecution jobExecution =  asyncJobLauncher.run(jobRegistry.getJob(jobName), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_RE_RUNS_FAILED)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Re-Run REGALG or TVRRUN Job for failed students from the given batchJobId", description = "Re-Run REGALG or TVRRUN Job for failed students from the given batchJobId", tags = { "RE-RUN" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchRerunFailed(@PathVariable Long batchId) {
        BatchJobResponse response = new BatchJobResponse();
        BatchGradAlgorithmJobHistoryEntity entity = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, entity.getTriggerBy());
        builder.addString(JOB_TYPE,  entity.getJobType());
        response.setJobType(entity.getJobType());
        response.setTriggerBy(entity.getTriggerBy());
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            boolean isSpecialRun = false;
            if (StringUtils.isNotBlank(entity.getJobParameters())) {
                builder.addString(SEARCH_REQUEST, entity.getJobParameters());
                response.setJobParameters(entity.getJobParameters());
                isSpecialRun = true;
            }
            builder.addString(RERUN_TYPE, RERUN_FAILED);
            builder.addLong(PREV_BATCH_ID, batchId);
            String jobName;
            switch(entity.getJobType()) {
                case REGALG:
                    if (isSpecialRun) {
                        jobName = SPECIAL_GRADUATION_BATCH_JOB;
                    } else {
                        jobName = GRADUATION_BATCH_JOB;
                    }
                    break;
                case TVRRUN:
                    if (isSpecialRun) {
                        jobName = SPECIAL_TVR_RUN_BATCH_JOB;
                    } else {
                        jobName = TVR_BATCH_JOB;
                    }
                    break;
                default:
                    jobName = "Not Known";
                    throw new NoSuchJobException(jobName);
            }
            JobExecution jobExecution =  asyncJobLauncher.run(jobRegistry.getJob(jobName), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_REGEN_SCHOOL_REPORTS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Re-Generate School Reports for the given batchJobId", description = "RRe-Generate School Reports for the given batchJobId", tags = { "RE-RUN" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<Boolean> launchRegenerateSchoolReports(@PathVariable Long batchId) {
        BatchGradAlgorithmJobHistoryEntity entity = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        if (entity != null) {
            try {
                logger.info(" Re-Generating School Reports for {} --------------------------------------------------------", entity.getJobType());
                List<UUID> uniqueSchoolList = gradBatchHistoryService.getSchoolListForReport(batchId);
                logger.info(" Number of Schools [{}] ---------------------------------------------------------", uniqueSchoolList.size());
                restUtils.createAndStoreSchoolReports(uniqueSchoolList, entity.getJobType());
                return ResponseEntity.ok(Boolean.TRUE);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Boolean.FALSE);
            }
        }
        return ResponseEntity.status(500).body(Boolean.FALSE);
    }

    /*@PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_REGEN_SCHOOL_REPORTS_BY_REQUEST)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Re-Generate School Reports for the given batchJobId", description = "Re-Generate School Reports for the given batchJobId", tags = { "RE-RUN" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<String> launchRegenerateSchoolReports(@RequestBody StudentSearchRequest searchRequest, @RequestParam(required = false) String type) {
        String schoolReportType = ObjectUtils.defaultIfNull(type, REGALG);
        logger.info(" Re-Generating School Reports by request for {} --------------------------------------------------------", schoolReportType);
        try {
            List<String> finalSchoolDistricts = gradSchoolOfRecordFilter.filterSchoolOfRecords(searchRequest).stream().sorted().toList();
            logger.info(" Number of Schools [{}] ---------------------------------------------------------", finalSchoolDistricts.size());
            int numberOfReports = restUtils.createAndStoreSchoolReports(finalSchoolDistricts, schoolReportType);
            return ResponseEntity.ok(numberOfReports + " school reports " + schoolReportType + " created successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getLocalizedMessage());
        }
    }*/

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_REGEN_SCHOOL_REPORTS_BY_REQUEST)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Re-Generate School Reports for the given batchJobId", description = "Re-Generate School Reports for the given batchJobId", tags = { "RE-RUN" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchRegenerateSchoolReportsBatch(@RequestBody StudentSearchRequest searchRequest) {
        logger.debug("launchSchoolReportRegenJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, SCHL_RPT_REGEN);
        response.setJobType(SCHL_RPT_REGEN);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            String studentSearchData = jsonTransformer.marshall(searchRequest);
            if(studentSearchData != null) {
                builder.addString(SEARCH_REQUEST, studentSearchData);
            }
            response.setJobParameters(studentSearchData);
            JobExecution jobExecution =  asyncJobLauncher.run(jobRegistry.getJob(REGENERATE_SCHOOL_REPORTS_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_REGEN_STUDENT_REPORTS_BY_REQUEST)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Re-Generate Student Reports for the given batchJobId", description = "Re-Generate Student Reports for the given batchJobId", tags = { "RE-RUN" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchRegenerateStudentReports(@RequestBody StudentSearchRequest searchRequest, @RequestParam String reportType) {
        logger.info(" Re-Generating Student Reports by request for {} --------------------------------------------------------", reportType);
        BatchJobResponse response = new BatchJobResponse();
        try {
            List<UUID> finalUUIDs = gradSchoolOfRecordFilter.filterStudents(searchRequest);
            logger.info(" Number of Students [{}] ---------------------------------------------------------", finalUUIDs.size());
            restUtils.processStudentReports(finalUUIDs, reportType);
            response.setStatus(BatchStatusEnum.COMPLETED.name());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_MONTHLY_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Monthly Distribution Runs", description = "Run Monthly Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchMonthlyDistributionRunJob() {
        return launchDistributionRunJob();
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Distribution Runs", description = "Run Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchDistributionRunJob() {
        return launchDistributionRunJob(null);
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Distribution Runs", description = "Run Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchDistributionRunJob(@RequestBody StudentSearchRequest request) {
        logger.debug("launchDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUN);
        try {
            if(request != null) {
                builder.addString(SEARCH_REQUEST, jsonTransformer.marshall(request));
            }
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob("DistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            if(summaryDTO == null) {
                summaryDTO = new DistributionSummaryDTO();
                jobContext.put(DISDTO, summaryDTO);
            }
            summaryDTO.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                 | JobParametersInvalidException | NoSuchJobException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_YEARLY_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Yearly Distribution Runs", description = "Run Yearly Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchYearlyDistributionRunJob(@RequestBody StudentSearchRequest request) {
        logger.debug("launchYearlyDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUN_YE);
        try {
            builder.addString(SEARCH_REQUEST, jsonTransformer.marshall(request));
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob("YearlyDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            if(summaryDTO == null) {
                summaryDTO = new DistributionSummaryDTO();
                jobContext.put(DISDTO, summaryDTO);
            }
            summaryDTO.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException |
                 JobParametersInvalidException | NoSuchJobException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SUPP_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Supplemental Distribution Runs", description = "Run Supplemental Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchSupplementalDistributionRunJob(@RequestBody StudentSearchRequest request) {
        logger.debug("launchSupplementallyDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUN_SUPP);
        try {
            builder.addString(SEARCH_REQUEST, jsonTransformer.marshall(request));
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob("SupplementalDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            if(summaryDTO == null) {
                summaryDTO = new DistributionSummaryDTO();
                jobContext.put(DISDTO, summaryDTO);
            }
            summaryDTO.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_YEARLY_NON_GRAD_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Year End Non Grad Distribution Runs", description = "Run Year End Non Grad Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchYearlyNonGradDistributionRunJob(@RequestBody StudentSearchRequest request) {
        logger.debug("launchYearlyNonGradDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, NONGRADRUN);
        try {
            builder.addString(SEARCH_REQUEST, jsonTransformer.marshall(request));
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob("YearlyNonGradDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            if(summaryDTO == null) {
                summaryDTO = new DistributionSummaryDTO();
                jobContext.put(DISDTO, summaryDTO);
            }
            summaryDTO.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException |
                 JobParametersInvalidException | NoSuchJobException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_SUMMARY)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    @Operation(summary = "Load Batch Summary", description = "Load Batch Summary", tags = { "Dashboard" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "204", description = "No Content")})
    public ResponseEntity<SummaryDashBoard> loadSummary(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber, @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        logger.debug("Inside loadSummary");
        SummaryDashBoard batchSummary = gradDashboardService.getBatchSummary(pageNumber,pageSize);
        if(batchSummary == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(batchSummary,HttpStatus.OK);
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_USER_REQ_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized TVR Runs", description = "Run specialized Distribution runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchUserReqDisRunSpecialJob(@PathVariable String credentialType, @RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchUserReqDisRunSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNUSER);
        builder.addString(LOCALDOWNLOAD,studentSearchRequest.getLocalDownload());
        DistributionSummaryDTO validate = validateInputDisRun(studentSearchRequest);
        if(validate != null) {
            return ResponseEntity.status(400).body(validate);
        }
        try {
            String studentSearchData = jsonTransformer.marshall(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            builder.addString(CREDENTIALTYPE,credentialType);
            JobExecution jobExecution =  jobLauncher.run(jobRegistry.getJob("UserReqDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            if(summaryDTO == null) {
                summaryDTO = new DistributionSummaryDTO();
                jobContext.put(DISDTO, summaryDTO);
            }
            summaryDTO.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }

    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_BLANK_USER_REQ_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized User Req Runs", description = "Run specialized Distribution runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BlankDistributionSummaryDTO> launchUserReqBlankDisRunSpecialJob(@RequestBody BlankCredentialRequest blankCredentialRequest, @PathVariable String credentialType) {
        logger.debug("launchUserReqDisRunSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNUSER);
        builder.addString(CREDENTIALTYPE, credentialType);
        builder.addString(LOCALDOWNLOAD, blankCredentialRequest.getLocalDownload());
        BlankDistributionSummaryDTO validate = validateInputBlankDisRun(blankCredentialRequest);
        if (validate != null) {
            return ResponseEntity.status(400).body(validate);
        }
        try {
            String studentSearchData = jsonTransformer.marshall(blankCredentialRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("blankDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            BlankDistributionSummaryDTO summaryDTO = (BlankDistributionSummaryDTO) jobContext.get("blankDistributionSummaryDTO");
            if(summaryDTO == null) {
                summaryDTO = new BlankDistributionSummaryDTO();
                jobContext.put("blankDistributionSummaryDTO", summaryDTO);
            }
            summaryDTO.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            BlankDistributionSummaryDTO summaryDTO = new BlankDistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_PSI_USER_REQ_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized User Req PSI Runs", description = "Run specialized PSI Distribution runs", tags = { "PSIs" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<PsiDistributionSummaryDTO> launchUserReqPsiDisRunSpecialJob(@RequestBody PsiCredentialRequest psiCredentialRequest, @PathVariable String transmissionType) {
        logger.debug("launchUserReqPsiDisRunSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, PSIDISTRUN);
        builder.addString(TRANMISSION_TYPE,transmissionType);
        if(transmissionType.equalsIgnoreCase("FTP")) {
            builder.addString(LOCALDOWNLOAD, "Y");
        }

        PsiDistributionSummaryDTO validate = validateInputPsiDisRun(psiCredentialRequest);
        if(validate != null) {
            return ResponseEntity.status(400).body(validate);
        }
        try {
            String studentSearchData = jsonTransformer.marshall(psiCredentialRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            JobExecution jobExecution =  asyncJobLauncher.run(jobRegistry.getJob("psiDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            PsiDistributionSummaryDTO summaryDTO = (PsiDistributionSummaryDTO)jobContext.get("psiDistributionSummaryDTO");
            if(summaryDTO == null) {
                summaryDTO = new PsiDistributionSummaryDTO();
                jobContext.put("psiDistributionSummaryDTO", summaryDTO);
            }
            summaryDTO.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException e) {
            PsiDistributionSummaryDTO summaryDTO = new PsiDistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    private PsiDistributionSummaryDTO validateInputPsiDisRun(PsiCredentialRequest psiCredentialRequest) {
        if(psiCredentialRequest.getPsiCodes().isEmpty() || StringUtils.isBlank(psiCredentialRequest.getPsiYear())) {
            PsiDistributionSummaryDTO summaryDTO = new PsiDistributionSummaryDTO();
            summaryDTO.setException("Please provide both parameters");
            return summaryDTO;
        }
        return null;
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_CERT_REGEN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Manual Cert Regen Job", description = "Run Manual Cert Regen Job", tags = { "Cert Regen" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchCertRegenJob() {
        logger.debug("launchCertRegenJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, CERT_REGEN);
        builder.addString(SEARCH_REQUEST, "");
        response.setJobType(CERT_REGEN);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob(CERTIFICATE_REGENERATION_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                 | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_CERT_REGEN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized User Req Cert Regen Job", description = "Run Specialized Cert Regen Job", tags = { "Cert Regen" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchUserReqCertRegenJob(@RequestBody CertificateRegenerationRequest certificateRegenerationRequest) {
        logger.debug("launchUserReqCertRegenJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, CERT_REGEN);
        response.setJobType(CERT_REGEN);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            String searchData = jsonTransformer.marshall(certificateRegenerationRequest);
            builder.addString(SEARCH_REQUEST, searchData);
            response.setJobParameters(searchData);
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob(CERTIFICATE_REGENERATION_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                 | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_EDW_SNAPSHOT_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run User Req EDW Snapshot Job", description = "Run User Req EDW Snapshot Job", tags = { "EDW Snapshot" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchEdwSnapshotJob(@RequestBody SnapshotRequest snapshotRequest) {
        logger.debug("launchUserReqEdwSnapshotJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, EDW_SNAPSHOT);
        if (snapshotRequest != null && snapshotRequest.getGradYear() == null) {
            snapshotRequest.setGradYear(Year.now().getValue());
        }
        if (snapshotRequest != null && StringUtils.isBlank(snapshotRequest.getOption())) {
            snapshotRequest.setOption("L");
        }
        response.setJobType(EDW_SNAPSHOT);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            String searchData = jsonTransformer.marshall(snapshotRequest);
            builder.addString(SEARCH_REQUEST, searchData);
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob(EDW_SNAPSHOT_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                 | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_ARCHIVE_SCHOOL_REPORTS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_ARCHIVE_SCHOOL_REPORTS)
    @Operation(summary = "Run Archive School Reports Batch Job", description = "Run Archive School Reports Batch Job", tags = { "Archive School Reports" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchArchiveSchoolReportsJob(@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchArchiveSchoolReporsJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        String userName = ThreadLocalStateUtil.getCurrentUser();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, userName);
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, ARCHIVE_SCHOOL_REPORTS);

        response.setJobType(ARCHIVE_SCHOOL_REPORTS);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            validateInputArchiveSchoolReports(studentSearchRequest);
            String searchData = jsonTransformer.marshall(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, StringUtils.defaultString(searchData, "{}"));
            Job job = jobRegistry.getJob(ARCHIVE_SCHOOL_REPORTS_BATCH_JOB);
            JobParameters jobParameters = job.getJobParametersIncrementer().getNext(builder.toJobParameters());
            JobExecution jobExecution = asyncJobLauncher.run(job, jobParameters);
            if(jobExecution != null) {
                ExecutionContext jobContext = jobExecution.getExecutionContext();
                DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
                summaryDTO.setBatchId(jobExecution.getId());
                summaryDTO.setUserName(userName);
                summaryDTO.setStudentSearchRequest(studentSearchRequest);
                jobContext.put(DISDTO, summaryDTO);
                response.setBatchId(jobExecution.getId());
            } else {
                response.setBatchId(jobParameters.getLong("run.id"));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                 | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_DELETE_STUDENT_REPORTS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_DELETE_STUDENT_REPORTS)
    @Operation(summary = "Run Archive School Reports Batch Job", description = "Run Archive School Reports Batch Job", tags = { "Archive School Reports" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchDeleteStudentReportsJob(@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchDeleteStudentReportsJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        String userName = ThreadLocalStateUtil.getCurrentUser();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, userName);
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DELETE_STUDENT_REPORTS);

        response.setJobType(DELETE_STUDENT_REPORTS);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            validateInputDeleteStudentReports(studentSearchRequest);
            String searchData = jsonTransformer.marshall(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, StringUtils.defaultString(searchData, "{}"));
            Job job = jobRegistry.getJob(DELETE_STUDENT_REPORTS_BATCH_JOB);
            JobParameters jobParameters = job.getJobParametersIncrementer().getNext(builder.toJobParameters());
            JobExecution jobExecution = asyncJobLauncher.run(job, jobParameters);
            if(jobExecution != null) {
                ExecutionContext jobContext = jobExecution.getExecutionContext();
                DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
                summaryDTO.setBatchId(jobExecution.getId());
                summaryDTO.setUserName(userName);
                summaryDTO.setStudentSearchRequest(studentSearchRequest);
                jobContext.put(DISDTO, summaryDTO);
                response.setBatchId(jobExecution.getId());
            } else {
                response.setBatchId(jobParameters.getLong("run.id"));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                 | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_YEARLY_ARCHIVE_STUDENTS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_ARCHIVE_STUDENTS)
    @Operation(summary = "Run Archive Students Batch Job", description = "Run Archive Students Batch Job", tags = { "Archive Students" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<BatchJobResponse> launchArchiveStudentsJob(@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchArchiveStudentsJob");
        BatchJobResponse response = new BatchJobResponse();
        JobParametersBuilder builder = new JobParametersBuilder();
        String userName = ThreadLocalStateUtil.getCurrentUser();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, userName);
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, ARCHIVE_STUDENTS);

        response.setJobType(ARCHIVE_STUDENTS);
        response.setTriggerBy(MANUAL);
        response.setStartTime(LocalDateTime.now());
        response.setStatus(BatchStatusEnum.STARTED.name());

        try {
            validateInputArchiveStudents(studentSearchRequest);
            String searchData = jsonTransformer.marshall(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, StringUtils.defaultString(searchData, "{}"));
            Job job = jobRegistry.getJob(ARCHIVE_STUDENTS_BATCH_JOB);
            JobParameters jobParameters = job.getJobParametersIncrementer().getNext(builder.toJobParameters());
            JobExecution jobExecution = asyncJobLauncher.run(job, jobParameters);
            if(jobExecution != null) {
                ExecutionContext jobContext = jobExecution.getExecutionContext();
                DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
                summaryDTO.setBatchId(jobExecution.getId());
                summaryDTO.setUserName(userName);
                summaryDTO.setStudentSearchRequest(studentSearchRequest);
                jobContext.put(DISDTO, summaryDTO);
                response.setBatchId(jobExecution.getId());
            } else {
                response.setBatchId(jobParameters.getLong("run.id"));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                 | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.NOTIFY_DISTRIBUTION_JOB_IS_COMPLETED)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Callback once Async Distribution process is completed", description = "Run Callback once Async Distribution process is completed", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<Void> notifyDistributionJobIsCompleted(
            @RequestParam(name = "batchId", defaultValue = "0") Long batchId,
            @RequestParam(name = "status", defaultValue = "success") String status,
            @RequestBody DistributionResponse distributionResponse) {

        if(("NONGRADYERUN".equalsIgnoreCase(distributionResponse.getActivityCode()) || "YEARENDDIST".equalsIgnoreCase(distributionResponse.getActivityCode()))) {
            restUtils.executePostDistribution(distributionResponse);
        }

        logger.debug("notifyDistributionJobIsCompleted: batchId [{}], status = {}", batchId, status);
        distributionRunStatusUpdateProcessor.process(batchId, status);
        logger.debug("distributionRunStatusUpdateProcessor is invoked: batchId [{}], status = {}", batchId, status);
        return ResponseEntity.ok(null);
    }

}
