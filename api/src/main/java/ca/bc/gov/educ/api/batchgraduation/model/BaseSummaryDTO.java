package ca.bc.gov.educ.api.batchgraduation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BaseSummaryDTO {
    // Total
    private Long batchId;
    private long totalReadCount = 0L;

    // Partition
    private long readCount = 0L;
    private long processedCount = 0L;

    private String exception;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String accessToken;
}
