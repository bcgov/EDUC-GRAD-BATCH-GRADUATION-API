package ca.bc.gov.educ.api.batchgraduation.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Search {
    /**
     * The Condition.  ENUM to hold and AND OR
     */
    Condition condition;

    /**
     * The Search criteria list.
     */
    List<SearchCriteria> searchCriteriaList;
}
