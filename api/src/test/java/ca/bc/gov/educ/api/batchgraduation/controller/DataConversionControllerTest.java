package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.ConversionSummaryDTO;
import ca.bc.gov.educ.api.batchgraduation.model.CourseRestriction;
import ca.bc.gov.educ.api.batchgraduation.service.DataConversionService;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class DataConversionControllerTest {

    @Mock
    private DataConversionService dataConversionService;

    @InjectMocks
    DataConversionController dataConversionController;

    @Test
    public void testRunCourseRestrictionsDataConversionJob() {
        CourseRestriction courseRestriction1 = new CourseRestriction();
        courseRestriction1.setMainCourse("main");
        courseRestriction1.setMainCourseLevel("12");
        courseRestriction1.setRestrictedCourse("rest");
        courseRestriction1.setRestrictedCourseLevel("12");

        CourseRestriction courseRestriction2 = new CourseRestriction();
        courseRestriction2.setMainCourse("CLEA");
        courseRestriction2.setMainCourseLevel("12");
        courseRestriction2.setRestrictedCourse("CLEB");
        courseRestriction2.setRestrictedCourseLevel("12");

        ConversionSummaryDTO summary = new ConversionSummaryDTO();
        summary.setTableName("GRAD_COURSE_RESTRICTIONS");

        Mockito.when(dataConversionService.loadInitialRawGradCourseRestrictionsData(true)).thenReturn(Arrays.asList(courseRestriction1, courseRestriction2));
        var result = dataConversionController.runCourseRestrictionsDataConversionJob(true);
        Mockito.verify(dataConversionService).loadInitialRawGradCourseRestrictionsData(true);

        assertThat(result).isNotNull();
        assertThat(result.getBody()).isNotNull();
        ConversionSummaryDTO responseSummary = result.getBody();
        assertThat(responseSummary.getReadCount()).isEqualTo(2L);
    }

}
