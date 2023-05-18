package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchStatusEnum;
import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradBatchHistoryService;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.PermissionsConstants;
import ca.bc.gov.educ.api.batchgraduation.util.ThreadLocalStateUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
@OpenAPIDefinition(info = @Info(title = "API for Manual Triggering of batch process.", description = "This API is for Manual Triggering of batch process.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"LOAD_STUDENT_IDS","LOAD_BATCH_DASHBOARD","RUN_GRAD_ALGORITHM"})})
public class JobLauncherController {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";
    private static final String SEARCH_REQUEST = "searchRequest";
    private static final String RERUN_TYPE = "reRunType";
    private static final String RUN_BY = "runBy";
    private static final String PREV_BATCH_ID = "previousBatchId";
    private static final String MANUAL = "MANUAL";
    private static final String TVRRUN = "TVRRUN";
    private static final String REGALG = "REGALG";
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

    private final JobLauncher jobLauncher;
    private final JobLauncher asyncJobLauncher;
    private final JobRegistry jobRegistry;
    private final RestUtils restUtils;
    private final GradDashboardService gradDashboardService;
    private final GradBatchHistoryService gradBatchHistoryService;

    @Autowired
    public JobLauncherController(
            JobLauncher jobLauncher,
            @Qualifier("asyncJobLauncher")
            JobLauncher asyncJobLauncher,
            JobRegistry jobRegistry,
            RestUtils restUtils,
            GradDashboardService gradDashboardService,
            GradBatchHistoryService gradBatchHistoryService) {
        this.jobLauncher = jobLauncher;
        this.asyncJobLauncher = asyncJobLauncher;
        this.jobRegistry = jobRegistry;
        this.restUtils = restUtils;
        this.gradDashboardService = gradDashboardService;
        this.gradBatchHistoryService = gradBatchHistoryService;
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
        response.setStartTime(new Date(System.currentTimeMillis()));
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
        response.setStartTime(new Date(System.currentTimeMillis()));
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
    public ResponseEntity<String> loadStudentIDs(@RequestBody List<LoadStudentData> loadStudentData,
                                                 @RequestHeader(name="Authorization") String accessToken) {
        logger.debug("Inside loadStudentIDs");
        Integer recordsAdded = restUtils.getStudentByPenFromStudentAPI(loadStudentData, accessToken.replace(BEARER, ""));
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
                                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                    @RequestHeader(name="Authorization") String accessToken) {
        logger.debug("Inside loadError");
        ErrorDashBoard dash = gradDashboardService.getErrorInfo(batchId,pageNumber,pageSize,accessToken.replace(BEARER, ""));
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
        response.setStartTime(new Date(System.currentTimeMillis()));
        response.setStatus(BatchStatusEnum.STARTED.name());
        validateInput(response, studentSearchRequest);
        if(response.getException() != null) {
            return ResponseEntity.status(400).body(response);
        }
        try {
            String studentSearchData = new ObjectMapper().writeValueAsString(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            response.setJobParameters(studentSearchData);
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob(SPECIAL_GRADUATION_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
            response.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private void validateInput(BatchJobResponse response, StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest.getPens().isEmpty() && studentSearchRequest.getDistricts().isEmpty() && studentSearchRequest.getSchoolCategoryCodes().isEmpty() && studentSearchRequest.getPrograms().isEmpty() && studentSearchRequest.getSchoolOfRecords().isEmpty()) {
            response.setException("Please provide at least 1 parameter");
        }
        response.setException(null);
    }

    private DistributionSummaryDTO validateInputDisRun(StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest.getPens().isEmpty() && studentSearchRequest.getDistricts().isEmpty() && studentSearchRequest.getSchoolCategoryCodes().isEmpty() && studentSearchRequest.getPrograms().isEmpty() && studentSearchRequest.getSchoolOfRecords().isEmpty()) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException("Please provide at least 1 parameter");
            return summaryDTO;
        }
        return null;
    }

    private BlankDistributionSummaryDTO validateInputBlankDisRun(BlankCredentialRequest blankCredentialRequest) {
        if(blankCredentialRequest.getSchoolOfRecords().isEmpty() || blankCredentialRequest.getCredentialTypeCode().isEmpty()) {
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
        response.setStartTime(new Date(System.currentTimeMillis()));
        response.setStatus(BatchStatusEnum.STARTED.name());
        validateInput(response, studentSearchRequest);
        if(response.getException() != null) {
            return ResponseEntity.status(400).body(response);
        }
        try {
            String studentSearchData = new ObjectMapper().writeValueAsString(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            response.setJobParameters(studentSearchData);
            JobExecution jobExecution =  asyncJobLauncher.run(jobRegistry.getJob(SPECIAL_TVR_RUN_BATCH_JOB), builder.toJobParameters());
            response.setBatchId(jobExecution.getId());
            return ResponseEntity.ok(response);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
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
        response.setStartTime(new Date(System.currentTimeMillis()));
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
        response.setStartTime(new Date(System.currentTimeMillis()));
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
    public ResponseEntity<Boolean> launchRegenerateSchoolReports(@PathVariable Long batchId, @RequestHeader(name="Authorization") String accessToken) {
        BatchGradAlgorithmJobHistoryEntity entity = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        if (entity != null) {
            try {
                logger.info(" Re-Generating School Reports for {} --------------------------------------------------------", entity.getJobType());
                List<String> uniqueSchoolList = gradBatchHistoryService.getSchoolListForReport(batchId);
                logger.info(" Number of Schools [{}] ---------------------------------------------------------", uniqueSchoolList.size());
                restUtils.createAndStoreSchoolReports(accessToken.replace(BEARER, ""), uniqueSchoolList, entity.getJobType());
                return ResponseEntity.ok(Boolean.TRUE);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(Boolean.FALSE);
            }
        }
        return ResponseEntity.status(500).body(Boolean.FALSE);
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
        logger.debug("launchDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUN);
        try {
            JobExecution jobExecution = asyncJobLauncher.run(jobRegistry.getJob("DistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_YEARLY_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Yearly Distribution Runs", description = "Run Yearly Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchYearlyDistributionRunJob() {
        logger.debug("launchYearlyDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUN_YE);
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("YearlyDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_SUPP_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Supplemental Distribution Runs", description = "Run Supplemental Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchSupplementalDistributionRunJob() {
        logger.debug("launchSupplementallyDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUN_SUPP);
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("SupplementalDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_YEARLY_NON_GRAD_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Year End Non Grad Distribution Runs", description = "Run Year End Non Grad Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchYearlyNonGradDistributionRunJob() {
        logger.debug("launchYearlyNonGradDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(RUN_BY, ThreadLocalStateUtil.getCurrentUser());
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, NONGRADRUN);
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("YearlyNonGradDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
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
    @Operation(summary = "Run Specialized TVR Runs", description = "Run specialized Distribution runs", tags = { "DISTRIBUTION" })
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
            String studentSearchData = new ObjectMapper().writeValueAsString(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            builder.addString(CREDENTIALTYPE,credentialType);
            JobExecution jobExecution =  jobLauncher.run(jobRegistry.getJob("UserReqDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            DistributionSummaryDTO summaryDTO = (DistributionSummaryDTO)jobContext.get(DISDTO);
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
            DistributionSummaryDTO summaryDTO = new DistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }

    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_BLANK_USER_REQ_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized User Req Runs", description = "Run specialized Distribution runs", tags = { "DISTRIBUTION" })
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
            String searchData = new ObjectMapper().writeValueAsString(blankCredentialRequest);
            builder.addString(SEARCH_REQUEST, searchData);
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("blankDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            BlankDistributionSummaryDTO summaryDTO = (BlankDistributionSummaryDTO) jobContext.get("blankDistributionSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
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
            String searchData = new ObjectMapper().writeValueAsString(psiCredentialRequest);
            builder.addString(SEARCH_REQUEST, searchData);
            JobExecution jobExecution =  asyncJobLauncher.run(jobRegistry.getJob("psiDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            PsiDistributionSummaryDTO summaryDTO = (PsiDistributionSummaryDTO)jobContext.get("psiDistributionSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
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

    @GetMapping(EducGradBatchGraduationApiConstants.NOTIFY_DISTRIBUTION_JOB_IS_COMPLETED)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Monthly Distribution Runs", description = "Run Monthly Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<Void> notifyDistributionJobIsCompleted(
            @RequestParam(name = "batchId", defaultValue = "0") Long batchId,
            @RequestParam(name = "status", defaultValue = "success") String status) {

        BatchGradAlgorithmJobHistoryEntity jobHistory = gradBatchHistoryService.getGradAlgorithmJobHistory(batchId);
        String jobType = jobHistory.getJobType();

        if (StringUtils.equalsIgnoreCase(status, "success")) {
            List<StudentCredentialDistribution> cList = gradBatchHistoryService.getStudentCredentialDistributions(batchId);

            // update graduation_student_record & student_certificate
            Map<String, ServiceException> unprocessed = updateBackStudentRecords(cList, batchId, getActivitCode(jobType));
            if (!unprocessed.isEmpty()) {
                status = BatchStatusEnum.FAILED.name();
                this.handleUnprocessedErrors(unprocessed);
            } else {
                status = BatchStatusEnum.COMPLETED.name();
            }
        } else {
            status = BatchStatusEnum.FAILED.name();
        }

        // update status for batch job history
        Date endTime = new Date(System.currentTimeMillis());
        jobHistory.setEndTime(endTime);
        jobHistory.setStatus(status);
        jobHistory.setJobParameters(populateJobParametersDTO(jobType, null));
        gradBatchHistoryService.saveGradAlgorithmJobHistory(jobHistory);

        return ResponseEntity.ok(null);
    }

    private Map<String, ServiceException> updateBackStudentRecords(List<StudentCredentialDistribution> cList, Long batchId, String activityCode) {
        Map<String, ServiceException> unprocessedStudents = new HashMap<>();
        cList.forEach(scd-> {
            try {
                final String token = restUtils.getTokenResponseObject().getAccess_token();
                restUtils.updateStudentCredentialRecord(scd.getStudentID(),scd.getCredentialTypeCode(),scd.getPaperType(),scd.getDocumentStatusCode(),activityCode,token);
                restUtils.updateStudentGradRecord(scd.getStudentID(),batchId,activityCode,token);
            } catch (Exception e) {
                unprocessedStudents.put(scd.getStudentID().toString(), (ServiceException) e);
            }
        });
        return unprocessedStudents;
    }

    private String getActivitCode(String jobType) {
        String activityCode = "MONTHLYDIST";
        if(StringUtils.isNotBlank(jobType)) {
            switch (jobType) {
                case DISTRUN -> activityCode = "MONTHLYDIST";
                case DISTRUN_YE -> activityCode = "YEARENDDIST";
                case DISTRUN_SUPP -> activityCode = "SUPPDIST";
                case NONGRADRUN -> activityCode = "NONGRADDIST";
            }
        }
        return activityCode;
    }

    private void handleUnprocessedErrors(Map<String, ServiceException> unprocessed) {
        unprocessed.forEach((k, v) -> logger.error("Student with id: {} did not have distribution date updated during monthly run due to: {}", k, v.getLocalizedMessage()));
    }

    private String populateJobParametersDTO(String jobType, String credentialType) {
        JobParametersForDistribution jobParamsDto = new JobParametersForDistribution();
        jobParamsDto.setJobName(jobType);
        jobParamsDto.setCredentialType(credentialType);

        String jobParamsDtoStr = null;
        try {
            jobParamsDtoStr = new ObjectMapper().writeValueAsString(jobParamsDto);
        } catch (Exception e) {
            logger.error("Job Parameters DTO parse error for User Request Distribution - {}", e.getMessage());
        }

        return jobParamsDtoStr;
    }
}
