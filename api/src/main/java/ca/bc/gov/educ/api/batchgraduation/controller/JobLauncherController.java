package ca.bc.gov.educ.api.batchgraduation.controller;

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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;

@Controller
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
public class JobLauncherController {

    private static Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private static final String TIME="time";
    private static final String JOB_PARAM="job";

    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private JobRegistry jobRegistry;
    
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
}
