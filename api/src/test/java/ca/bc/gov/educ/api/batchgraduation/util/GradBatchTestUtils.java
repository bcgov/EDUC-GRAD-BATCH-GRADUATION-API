package ca.bc.gov.educ.api.batchgraduation.util;

import ca.bc.gov.educ.api.batchgraduation.entity.ConvGradStudentEntity;
import ca.bc.gov.educ.api.batchgraduation.mappers.ConvGradStudentMapper;
import ca.bc.gov.educ.api.batchgraduation.model.ConvGradStudent;
import ca.bc.gov.educ.api.batchgraduation.repository.ConvGradStudentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@Profile("test")
public class GradBatchTestUtils {
    @Autowired
    ConvGradStudentRepository convGradStudentRepository;

//    @Autowired
//    GradCourseRestrictionRepository gradCourseRestrictionRepository;

    @Autowired
    private ConvGradStudentMapper mapper;

    public static ConvGradStudentEntity populateIdAndAuditColumns(final ConvGradStudentEntity entity) {
        if (entity.getStudentID() == null) {
            entity.setStudentID(UUID.randomUUID());
        }
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy("BATCH-GRADUATION-API");
        }
        if (entity.getUpdatedBy() == null) {
            entity.setUpdatedBy("BATCH-GRADUATION-API");
        }

        entity.setCreatedTimestamp(new Date(System.currentTimeMillis()));
        entity.setUpdatedTimestamp(new Date(System.currentTimeMillis()));

        return entity;
    }

    public List<ConvGradStudent> createConvGradStudents(final String jsonFileName) throws IOException {
        final File file = new File(
                Objects.requireNonNull(GradBatchTestUtils.class.getClassLoader().getResource(jsonFileName)).getFile()
        );
        final List<ConvGradStudent> models = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
//        final var entities = models.stream().map(mapper::toEntity)
//                .collect(toList()).stream().map(GradBatchTestUtils::populateIdAndAuditColumns).collect(toList());

//        dataConversionRepository.saveAll(entities);
        return models;
    }


}
