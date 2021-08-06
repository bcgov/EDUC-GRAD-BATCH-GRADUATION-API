package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.repository.TraxStudentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
@Profile("test")
public class GradBatchTestUtils {

    public List<ConvGradStudent> createConvGradStudents(final String jsonFileName) throws IOException {
        final File file = new File(
                Objects.requireNonNull(GradBatchTestUtils.class.getClassLoader().getResource(jsonFileName)).getFile()
        );
        final List<ConvGradStudent> models = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        return models;
    }


}
