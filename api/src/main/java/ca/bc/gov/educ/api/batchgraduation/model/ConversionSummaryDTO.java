package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ConversionSummaryDTO {

  private String tableName;

  private long readCount = 0;
  private long processedCount = 0;

  private long addedCount = 0;
  private long updatedCount = 0;

  private List<ConversionError> errors = new ArrayList<>();
  private String exception;

  // stats
  private Map<String, Long> programCountMap = new HashMap<>() {{
    put("2018-EN", 0L);
    put("2018-PF", 0L);
    put("2004", 0L);
    put("1996", 0L);
    put("1986", 0L);
    put("1950-EN", 0L);
    put("NOPROG", 0L);
    put("SCCP", 0L);
  }};

  public void increment(String programCode) {
    Long count = programCountMap.get(programCode);
    if (count != null) {
      count++;
      programCountMap.put(programCode, count);
    }
  }
}
