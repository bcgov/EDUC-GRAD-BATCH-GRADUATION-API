package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@Component
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PsiCredentialRequest implements Serializable {
    private List<String> psiCodes;
    private List<UUID> psiIds; // TODO: PSI GUID will be populated from STS
    private String psiYear;

}
