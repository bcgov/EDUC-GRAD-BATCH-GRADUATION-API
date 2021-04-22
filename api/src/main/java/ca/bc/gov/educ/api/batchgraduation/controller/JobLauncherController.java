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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.service.GradStudentService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.PermissionsContants;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
@EnableResourceServer
public class JobLauncherController {

    private static Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private static final String TIME="time";
    private static final String JOB_PARAM="job";

    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private JobRegistry jobRegistry;
    
    @Autowired
    private GradStudentService gradStudentService;
    
    @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_BATCH_JOB)
    public void launchJob( ) {
    	logger.info("Inside Launch Job");
    	JobParametersBuilder builder = new JobParametersBuilder();
    	builder.addLong(TIME, System.currentTimeMillis()).toJobParameters();
    	builder.addString(JOB_PARAM, "GraduationBatchJob");
    	try {
			jobLauncher.run(jobRegistry.getJob("GraduationBatchJob"), builder.toJobParameters());
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException | NoSuchJobException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    }
    
    @PostMapping(EducGradBatchGraduationApiConstants.LOAD_STUDENT_IDS)
    @PreAuthorize(PermissionsContants.LOAD_STUDENT_IDS)
    public void loadStudentIDs(@RequestBody List<LoadStudentData> loadStudentData) {
    	logger.info("Inside loadStudentIDs");
    	OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails(); 
    	String accessToken = auth.getTokenValue();
    	gradStudentService.getStudentByPenFromStudentAPI(loadStudentData,accessToken);
    	
    }
}
