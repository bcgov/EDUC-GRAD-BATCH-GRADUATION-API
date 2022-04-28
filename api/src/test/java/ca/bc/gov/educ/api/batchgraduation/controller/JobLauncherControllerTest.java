package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.LoadStudentData;
import ca.bc.gov.educ.api.batchgraduation.model.StudentSearchRequest;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
@ExtendWith(MockitoExtension.class)
public class JobLauncherControllerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private JobRegistry jobRegistry;

    @Mock
    private JobParametersBuilder jobParametersBuilder;

    @InjectMocks
    private JobLauncherController jobLauncherController;

    @Test
    public void testRegGradJob() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchRegGradJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testTVRJob() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchTvrRunJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }

    /*@Test
    public void testLoadStudentIDs() {
        // ID
        UUID studentID = UUID.randomUUID();
        String pen = "123456789";

        LoadStudentData loadStudentData = new LoadStudentData();
        loadStudentData.setPen(pen);

        Authentication authentication = Mockito.mock(Authentication.class);
        OAuth2AuthenticationDetails details = Mockito.mock(OAuth2AuthenticationDetails.class);
        // Mockito.whens() for your authorization object
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        Mockito.when(authentication.getDetails()).thenReturn(details);
        SecurityContextHolder.setContext(securityContext);

        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.loadStudentIDs(Arrays.asList(loadStudentData), "");
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();

    }*/

    @Test
    public void testDistributionGradJob() {
        boolean exceptionIsThrown = false;
        try {
            jobLauncherController.launchDistributionRunJob();
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testSpcTVRGradJob() {
        boolean exceptionIsThrown = false;
        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchTvrRunSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }

    @Test
    public void testSpcRegGradJob() {
        boolean exceptionIsThrown = false;
        StudentSearchRequest req = new StudentSearchRequest();
        req.setPens(Arrays.asList("123213123"));
        try {
            jobLauncherController.launchRegGradSpecialJob(req);
        } catch (Exception e) {
            exceptionIsThrown = true;
        }

        assertThat(exceptionIsThrown).isTrue();
    }
}
