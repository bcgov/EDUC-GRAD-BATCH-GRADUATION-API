package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.service.DataConversionService;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import ca.bc.gov.educ.api.batchgraduation.util.PermissionsContants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING + EducGradBatchGraduationApiConstants.GRAD_CONVERSION_API_MAPPING)
@CrossOrigin
@EnableResourceServer
public class DataConversionController {
  private static Logger logger = LoggerFactory.getLogger(DataConversionController.class);

  @Autowired
  private DataConversionService dataConversionService;

  @GetMapping(EducGradBatchGraduationApiConstants.EXECUTE_COURSE_RESTRICTIONS_CONVERSION_JOB)
  @PreAuthorize(PermissionsContants.LOAD_STUDENT_IDS)
  public ResponseEntity<ConversionSummaryDTO> runCourseRestrictionsDataConversionJob(@RequestParam(defaultValue = "true") boolean purge) throws Exception {
    logger.info("Inside runDataConversionJob");
    OAuth2AuthenticationDetails auth = (OAuth2AuthenticationDetails) SecurityContextHolder.getContext().getAuthentication().getDetails();
    String accessToken = auth.getTokenValue();

    ConversionSummaryDTO summary = new ConversionSummaryDTO();
    summary.setTableName("COURSE RESTRICTIONS");

    try {
      dataConversionService.loadInitialRawGradCourseRestrictionsData(purge);
      logger.info("01. Course Restrictions - Initial Raw Data Load is done successfully");
    } catch (Exception e) {
      logger.info("01. Initial Raw Data Loading is failed: " + e.getLocalizedMessage());
      e.printStackTrace();
      summary.setException(e.getLocalizedMessage());
      ResponseEntity.status(500).body(summary);
    }

    try {
      dataConversionService.updateCourseRestrictions(summary);
      logger.info("02. Update Data for date conversion is done successfully");
    } catch (Exception e) {
      logger.info("02. Update Data for date conversion is failed: " + e.getLocalizedMessage());
      e.printStackTrace();
      summary.setException(e.getLocalizedMessage());
      ResponseEntity.status(500).body(summary);
    }
    return ResponseEntity.ok(summary);
  }
}
