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
public class BlankCredentialRequest implements Serializable {
    private List<String> schoolOfRecords;
    private List<String> credentialTypeCode;
    private String user;
    private Address address;
    private int quantity;
    String localDownload;
}
