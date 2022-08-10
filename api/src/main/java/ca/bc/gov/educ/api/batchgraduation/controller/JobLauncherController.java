package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.PermissionsConstants;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private static final String MANUAL = "MANUAL";
    private static final String TVRRUN = "TVRRUN";
    private static final String REGALG = "REGALG";
    private static final String DISTRUNMONTH = "DISTRUNMONTH";
    private static final String DISTRUNYEAREND = "DISTRUNYEAREND";
    private static final String DISTRUNUSER = "DISTRUNUSER";
    private static final String PSIDISTRUN = "PSIRUN";
    private static final String CREDENTIALTYPE = "credentialType";
    private static final String TRANMISSION_TYPE = "transmissionType";
    private static final String DISDTO = "distributionSummaryDTO";
    private static final String SCHREPORT = "SCHREP";
    private static final String LOCALDOWNLOAD = "LocalDownload";

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final RestUtils restUtils;
    private final GradDashboardService gradDashboardService;

    public JobLauncherController(JobLauncher jobLauncher, JobRegistry jobRegistry, RestUtils restUtils, GradDashboardService gradDashboardService) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.restUtils = restUtils;
        this.gradDashboardService = gradDashboardService;
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_REG_GRAD_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Manual Reg Grad Job", description = "Run Manual Reg Grad Job", tags = { "Reg Grad" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<AlgorithmSummaryDTO> launchRegGradJob() {
        logger.debug("launchRegGradJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, REGALG);

        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("GraduationBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("regGradAlgSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }

    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_TVR_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Manual TVR Job", description = "Run Manual TVR Job", tags = { "TVR" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<AlgorithmSummaryDTO> launchTvrRunJob() {
        logger.debug("launchTvrRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, TVRRUN);
        try {
            JobExecution jobExecution =jobLauncher.run(jobRegistry.getJob("tvrBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("tvrRunSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException | IllegalArgumentException e) {
            AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.LOAD_STUDENT_IDS)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    @Operation(summary = "Load Students to GRAD", description = "Load Students to GRAD", tags = { "Student" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<String> loadStudentIDs(@RequestBody List<LoadStudentData> loadStudentData,
                                                 @RequestHeader(name="Authorization") String accessToken) {
        logger.debug("Inside loadStudentIDs");
        Integer recordsAdded = restUtils.getStudentByPenFromStudentAPI(loadStudentData, accessToken.replace("Bearer ", ""));
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
        ErrorDashBoard dash = gradDashboardService.getErrorInfo(batchId,pageNumber,pageSize,accessToken.replace("Bearer ", ""));
        if(dash == null) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(dash,HttpStatus.OK);
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized Regular Grad Runs", description = "Run specialized Regular Grad runs", tags = { "Reg Grad" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<AlgorithmSummaryDTO> launchRegGradSpecialJob(@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchRegGradSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, REGALG);
        AlgorithmSummaryDTO validate = validateInput(studentSearchRequest);
        if(validate != null) {
            return ResponseEntity.status(400).body(validate);
        }
        try {
            String studentSearchData = new ObjectMapper().writeValueAsString(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("SpecialGraduationBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("regGradAlgSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
            AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }
    }

    private AlgorithmSummaryDTO validateInput(StudentSearchRequest studentSearchRequest) {
        if(studentSearchRequest.getPens().isEmpty() && studentSearchRequest.getDistricts().isEmpty() && studentSearchRequest.getSchoolCategoryCodes().isEmpty() && studentSearchRequest.getPrograms().isEmpty() && studentSearchRequest.getSchoolOfRecords().isEmpty()) {
            AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
            summaryDTO.setException("Please provide at least 1 parameter");
            return summaryDTO;
        }
        return null;
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
    public ResponseEntity<AlgorithmSummaryDTO> launchTvrRunSpecialJob(@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchTvrRunSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, TVRRUN);
        AlgorithmSummaryDTO validate = validateInput(studentSearchRequest);
        if(validate != null) {
            return ResponseEntity.status(400).body(validate);
        }
        try {
            String studentSearchData = new ObjectMapper().writeValueAsString(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            JobExecution jobExecution =  jobLauncher.run(jobRegistry.getJob("SpecialTvrRunBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            AlgorithmSummaryDTO summaryDTO = (AlgorithmSummaryDTO)jobContext.get("tvrRunSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
            AlgorithmSummaryDTO summaryDTO = new AlgorithmSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }

    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Monthly Distribution Runs", description = "Run Monthly Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchDistributionRunJob() {
        logger.debug("launchDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNMONTH);
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("DistributionBatchJob"), builder.toJobParameters());
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
    @Operation(summary = "Run Monthly Distribution Runs", description = "Run Monthly Distribution Runs", tags = { "Distribution" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<DistributionSummaryDTO> launchYearlyDistributionRunJob() {
        logger.debug("launchYearlyDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNYEAREND);
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
    public ResponseEntity<DistributionSummaryDTO> launchUserReqDisRunSpecialJob(@PathVariable String credentialType,@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchUserReqDisRunSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
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
    public ResponseEntity<BlankDistributionSummaryDTO> launchUserReqBlankDisRunSpecialJob(@RequestBody BlankCredentialRequest blankCredentialRequest,@PathVariable String credentialType) {
        logger.debug("launchUserReqDisRunSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, DISTRUNUSER);
        builder.addString(CREDENTIALTYPE,credentialType);
        builder.addString(LOCALDOWNLOAD,blankCredentialRequest.getLocalDownload());
        BlankDistributionSummaryDTO validate = validateInputBlankDisRun(blankCredentialRequest);
        if(validate != null) {
            return ResponseEntity.status(400).body(validate);
        }
        try {
            String searchData = new ObjectMapper().writeValueAsString(blankCredentialRequest);
            builder.addString(SEARCH_REQUEST, searchData);
            JobExecution jobExecution =  jobLauncher.run(jobRegistry.getJob("blankDistributionBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            BlankDistributionSummaryDTO summaryDTO = (BlankDistributionSummaryDTO)jobContext.get("blankDistributionSummaryDTO");
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
            BlankDistributionSummaryDTO summaryDTO = new BlankDistributionSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }

    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_SCHOOL_REPORT_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Nightly School Report Runs", description = "Run Nightly School Report Runs", tags = { "School Report" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<SchoolReportSummaryDTO> launchSchoolReportRunJob() {
        logger.debug("launchSchoolReportRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, SCHREPORT);
        try {
            JobExecution jobExecution = jobLauncher.run(jobRegistry.getJob("SchoolReportBatchJob"), builder.toJobParameters());
            ExecutionContext jobContext = jobExecution.getExecutionContext();
            SchoolReportSummaryDTO summaryDTO = (SchoolReportSummaryDTO)jobContext.get(DISDTO);
            return ResponseEntity.ok(summaryDTO);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            SchoolReportSummaryDTO summaryDTO = new SchoolReportSummaryDTO();
            summaryDTO.setException(e.getLocalizedMessage());
            return ResponseEntity.status(500).body(summaryDTO);
        }

    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_PSI_USER_REQ_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    @Operation(summary = "Run Specialized User Req PSI Runs", description = "Run specialized PSI Distribution runs", tags = { "PSIs" })
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),@ApiResponse(responseCode = "500", description = "Internal Server Error")})
    public ResponseEntity<PsiDistributionSummaryDTO> launchUserReqPsiDisRunSpecialJob(@RequestBody PsiCredentialRequest psiCredentialRequest,@PathVariable String transmissionType) {
        logger.debug("launchUserReqPsiDisRunSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, MANUAL);
        builder.addString(JOB_TYPE, PSIDISTRUN);
        builder.addString(TRANMISSION_TYPE,transmissionType);
        if(transmissionType.equalsIgnoreCase("FTP"))
            builder.addString(LOCALDOWNLOAD,"Y");

        PsiDistributionSummaryDTO validate = validateInputPsiDisRun(psiCredentialRequest);
        if(validate != null) {
            return ResponseEntity.status(400).body(validate);
        }
        try {
            String searchData = new ObjectMapper().writeValueAsString(psiCredentialRequest);
            builder.addString(SEARCH_REQUEST, searchData);
            JobExecution jobExecution =  jobLauncher.run(jobRegistry.getJob("psiDistributionBatchJob"), builder.toJobParameters());
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
}
