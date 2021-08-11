package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
@Profile("test")
public class GradBatchTestUtils {

    public List<GraduationStatus> createConvGradStudents(final String jsonFileName) throws IOException {
        final File file = new File(
                Objects.requireNonNull(GradBatchTestUtils.class.getClassLoader().getResource(jsonFileName)).getFile()
        );
        final List<GraduationStatus> models = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        return models;
    }


}
