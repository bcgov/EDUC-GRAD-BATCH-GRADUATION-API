package ca.bc.gov.educ.api.batchgraduation.controller;

import java.util.List;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.*;

import ca.bc.gov.educ.api.batchgraduation.model.GradDashboard;
import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.service.GradDashboardService;
import ca.bc.gov.educ.api.batchgraduation.service.GradStudentService;
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

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final GradStudentService gradStudentService;
    private final GradDashboardService gradDashboardService;

    public JobLauncherController(JobLauncher jobLauncher, JobRegistry jobRegistry, GradStudentService gradStudentService, GradDashboardService gradDashboardService) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.gradStudentService = gradStudentService;
        this.gradDashboardService = gradDashboardService;
    }

    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_REG_GRAD_BATCH_JOB)
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
        gradStudentService.getStudentByPenFromStudentAPI(loadStudentData, accessToken);

    }

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_DASHBOARD)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    public GradDashboard loadDashboard() {
        logger.info("Inside loadDashboard");
        return gradDashboardService.getDashboardInfo();

    }
}
