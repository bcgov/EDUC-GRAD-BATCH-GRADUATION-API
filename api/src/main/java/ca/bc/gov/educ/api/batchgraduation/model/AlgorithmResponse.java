package ca.bc.gov.educ.api.batchgraduation.model;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class AlgorithmResponse {

    private GraduationStatus graduationStatus;
    private List<StudentOptionalProgram> specialGraduationStatus;
}
