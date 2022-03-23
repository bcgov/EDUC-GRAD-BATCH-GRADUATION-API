package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSearchRequest implements Serializable {
    List<String> schoolOfRecords;
    List<String> districts;
    List<String> pens;
    List<String> programs;

    Boolean validateInput;
}
