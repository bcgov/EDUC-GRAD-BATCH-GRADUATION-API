package ca.bc.gov.educ.api.batchgraduation.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class EducGradBatchGraduationApiUtilsTest {

    @Test
    public void testParsingTraxDate_whenDashFormat_thenReturnsMonthEnd() {
        Date result = EducGradBatchGraduationApiUtils.parsingTraxDate("2026-04");

        assertThat(EducGradBatchGraduationApiUtils.getProgramCompletionDate(result)).isEqualTo("2026-04-30");
    }

    @Test
    public void testParsingTraxDate_whenSlashFormat_thenReturnsMonthEnd() {
        Date result = EducGradBatchGraduationApiUtils.parsingTraxDate("2026/04");

        assertThat(EducGradBatchGraduationApiUtils.getProgramCompletionDate(result)).isEqualTo("2026-04-30");
    }

    @Test
    public void testNormalizeProgramCompletionMonth_whenDashFormat_thenReturnsSlashFormat() throws Exception {
        Method method = EducGradBatchGraduationApiUtils.class.getDeclaredMethod("normalizeProgramCompletionMonth", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "2026-04");

        assertThat(result).isEqualTo("2026/04");
    }

    @Test
    public void testNormalizeProgramCompletionMonth_whenSlashFormat_thenReturnsSameValue() throws Exception {
        Method method = EducGradBatchGraduationApiUtils.class.getDeclaredMethod("normalizeProgramCompletionMonth", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "2026/04");

        assertThat(result).isEqualTo("2026/04");
    }

    @Test
    public void testNormalizeProgramCompletionMonth_whenNull_thenReturnsNull() throws Exception {
        Method method = EducGradBatchGraduationApiUtils.class.getDeclaredMethod("normalizeProgramCompletionMonth", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, new Object[]{null});

        assertThat(result).isNull();
    }
}
