package ca.bc.gov.educ.api.batchgraduation.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchJobRequest {
    List<String> districts = new ArrayList<>();
    List<String> schoolCategories = new ArrayList<>();
    List<String> mincodes = new ArrayList<>();
}
