package ca.bc.gov.educ.api.batchgraduation.controller;

import java.util.List;

import ca.bc.gov.educ.api.batchgraduation.model.ErrorBoard;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import ca.bc.gov.educ.api.batchgraduation.rest.RestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.*;

import ca.bc.gov.educ.api.batchgraduation.model.GradDashboard;
import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.PermissionsConstants;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
@EnableResourceServer
public class JobLauncherController {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private static final String TIME = "time";
    private static final String JOB_TRIGGER="jobTrigger";
    private static final String JOB_TYPE="jobType";
    private static final String SEARCH_REQUEST = "searchRequest";

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
    public void launchRegGradJob() {
        logger.debug("launchRegGradJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "MANUAL");
        builder.addString(JOB_TYPE, "REGALG");
        try {
            jobLauncher.run(jobRegistry.getJob("GraduationBatchJob"), builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_TVR_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    public void launchTvrRunJob() {
        logger.debug("launchTvrRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "MANUAL");
        builder.addString(JOB_TYPE, "TVRRUN");
        try {
            jobLauncher.run(jobRegistry.getJob("tvrBatchJob"), builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @PostMapping(EducGradBatchGraduationApiConstants.LOAD_STUDENT_IDS)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    public void loadStudentIDs(@RequestBody List<LoadStudentData> loadStudentData) {
        logger.info("Inside loadStudentIDs");
        OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String accessToken = auth.getTokenValue();
        restUtils.getStudentByPenFromStudentAPI(loadStudentData, accessToken);

    }

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_DASHBOARD)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    public GradDashboard loadDashboard() {
        logger.info("Inside loadDashboard");
        return gradDashboardService.getDashboardInfo();

    }

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_ERRORS)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    public ResponseEntity<List<ErrorBoard>> loadError(@PathVariable Long batchId, @RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber, @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize) {
        logger.info("Inside loadError");
        OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
        String accessToken = auth.getTokenValue();
        List<ErrorBoard> errList = gradDashboardService.getErrorInfo(batchId,pageNumber,pageSize,accessToken);
        if(errList.isEmpty()) {
            return new ResponseEntity<List<ErrorBoard>>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<List<ErrorBoard>>(errList,HttpStatus.OK);
    }

    @PostMapping(EducGradBatchGraduationApiConstants.EXECUTE_SPECIALIZED_RUNS)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    public void launchRegGradSpecialJob(@RequestBody StudentSearchRequest studentSearchRequest) {
        logger.debug("launchRegGradSpecialJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "MANUAL");
        builder.addString(JOB_TYPE, "REGALG");

        try {
            String studentSearchData = new ObjectMapper().writeValueAsString(studentSearchRequest);
            builder.addString(SEARCH_REQUEST, studentSearchData);
            jobLauncher.run(jobRegistry.getJob("SpecialGraduationBatchJob"), builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException | NoSuchJobException | JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_DIS_RUN_BATCH_JOB)
    @PreAuthorize(PermissionsConstants.RUN_GRAD_ALGORITHM)
    public void launchDistributionRunJob() {
        logger.debug("launchDistributionRunJob");
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
        builder.addString(JOB_TRIGGER, "MANUAL");
        builder.addString(JOB_TYPE, "DISTRUNMONTH");
        try {
            jobLauncher.run(jobRegistry.getJob("DistributionBatchJob"), builder.toJobParameters());
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
                | JobParametersInvalidException | NoSuchJobException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
