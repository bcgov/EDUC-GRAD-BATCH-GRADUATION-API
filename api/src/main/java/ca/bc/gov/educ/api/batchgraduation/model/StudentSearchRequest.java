package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSearchRequest implements Serializable {
    private List<String> schoolOfRecords = new ArrayList<>();
    private List<String> districts = new ArrayList<>();
    private List<String> schoolCategoryCodes = new ArrayList<>();
    private List<String> pens = new ArrayList<>();
    private List<String> programs = new ArrayList<>();
    private List<UUID> studentIDs = new ArrayList<>();
    private List<String> statuses = new ArrayList<>();
    private List<String> reportTypes = new ArrayList();

    private String user;
    private Address address;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate gradDateFrom;
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate gradDateTo;

    Boolean validateInput;
    String activityCode;
    String localDownload;
    Integer quantity;

    @JsonIgnore
    public boolean isEmpty() {
        return  (schoolOfRecords == null || schoolOfRecords.isEmpty()) &&
                (districts == null || districts.isEmpty()) &&
                (schoolCategoryCodes == null || schoolCategoryCodes.isEmpty()) &&
                (pens == null || pens.isEmpty()) &&
                (studentIDs == null || studentIDs.isEmpty());
    }

}
