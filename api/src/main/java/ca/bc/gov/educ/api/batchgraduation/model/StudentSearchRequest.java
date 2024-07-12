package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSearchRequest implements Serializable {
    private List<String> schoolOfRecords;
    private List<String> districts;
    private List<String> schoolCategoryCodes;
    private List<String> pens;
    private List<String> programs;
    private List<UUID> studentIDs;
    private List<String> statuses;

    private String user;
    private Address address;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate gradDateFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate gradDateTo;

    Boolean validateInput;
    String activityCode;
    String localDownload;
}
