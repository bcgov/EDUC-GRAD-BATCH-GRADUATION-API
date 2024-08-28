package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseSummaryDTO implements Serializable {
    private Long batchId;

    // partition
    private long readCount = 0L;
    private long processedCount = 0L;
    private long erroredCount = 0L;

    private String exception;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String accessToken;

    private String userName;
}
