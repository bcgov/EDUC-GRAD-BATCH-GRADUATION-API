package ca.bc.gov.educ.api.batchgraduation.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.bc.gov.educ.api.batchgraduation.model.GraduationStudentRecord;

@Component
@Profile("test")
public class GradBatchTestUtils {

    public List<GraduationStudentRecord> createConvGradStudents(final String jsonFileName) throws IOException {
        final File file = new File(
                Objects.requireNonNull(GradBatchTestUtils.class.getClassLoader().getResource(jsonFileName)).getFile()
        );
        final List<GraduationStudentRecord> models = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        return models;
    }


}
