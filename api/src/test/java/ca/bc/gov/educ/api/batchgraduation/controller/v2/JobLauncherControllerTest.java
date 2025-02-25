package ca.bc.gov.educ.api.batchgraduation.controller.v2;

import ca.bc.gov.educ.api.batchgraduation.EducGradBatchGraduationApplication;
import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.filter.FilterOperation;
import ca.bc.gov.educ.api.batchgraduation.model.dto.Search;
import ca.bc.gov.educ.api.batchgraduation.model.dto.SearchCriteria;
import ca.bc.gov.educ.api.batchgraduation.model.dto.ValueType;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.util.EducGradBatchGraduationApiConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.api.batchgraduation.model.dto.Condition.AND;
import static ca.bc.gov.educ.api.batchgraduation.model.dto.Condition.OR;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = { EducGradBatchGraduationApplication.class })
@ActiveProfiles("test")
@AutoConfigureMockMvc
class JobLauncherControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  BatchGradAlgorithmJobHistoryRepository repository;

  protected static final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();

  @BeforeEach
  void setUp() {
    this.repository.deleteAll();
    var entity1 = createJobHistoryEntity();
    var entity2 = createJobHistoryEntity();
    entity2.setStatus("FAILED");
    repository.saveAll(List.of(entity1, entity2));
  }

  @Test
  void testGetJobHistory_givenSearchCriteria_returnsCorrectly() throws Exception {
    final SearchCriteria searchCriteria1 = SearchCriteria.builder()
        .condition(AND)
        .key("status")
        .operation(FilterOperation.EQUAL)
        .value("FAILED")
        .valueType(ValueType.STRING)
        .build();

    final SearchCriteria searchCriteria2 = SearchCriteria.builder()
        .condition(AND)
        .key("endTime")
        .operation(FilterOperation.LESS_THAN)
        .value(LocalDateTime.now().toString())
        .valueType(ValueType.DATE_TIME)
        .build();

    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(searchCriteria1);
    criteriaList.add(searchCriteria2);

    final SearchCriteria searchCriteria3 = SearchCriteria.builder()
        .condition(OR)
        .key("jobExecutionId")
        .operation(FilterOperation.BETWEEN)
        .value("124,125")
        .valueType(ValueType.INTEGER)
        .build();

    final SearchCriteria searchCriteria4 = SearchCriteria.builder()
        .condition(OR)
        .key("failedStudentsProcessed")
        .operation(FilterOperation.IN)
        .value("57,58,59")
        .valueType(ValueType.LONG)
        .build();

    final SearchCriteria searchCriteria5 = SearchCriteria.builder()
        .condition(OR)
        .key("failedStudentsProcessed")
        .operation(FilterOperation.NOT_EQUAL)
        .value("5")
        .valueType(ValueType.LONG)
        .build();

    final List<SearchCriteria> criteriaList2 = new ArrayList<>();
    criteriaList.add(searchCriteria3);
    criteriaList.add(searchCriteria4);
    criteriaList.add(searchCriteria5);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).condition(AND).build());
    searches.add(Search.builder().searchCriteriaList(criteriaList2).condition(AND).build());

    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_V2_ROOT_MAPPING + EducGradBatchGraduationApiConstants.BATCH_DASHBOARD)
            .with(jwt().jwt(jwt -> jwt.claim("scope", "LOAD_STUDENT_IDS")))
            .param("searchParams", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.content", hasSize(1)));
  }

  @Test
  void testGetJobHistory_givenNoResult_returnsPageableWithNothing() throws Exception {
    final SearchCriteria searchCriteria1 = SearchCriteria.builder()
        .condition(AND)
        .key("status")
        .operation(FilterOperation.EQUAL)
        .value("SUCCESS")
        .valueType(ValueType.STRING)
        .build();

    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(searchCriteria1);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());

    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    final MvcResult result = this.mockMvc
        .perform(get(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_V2_ROOT_MAPPING + EducGradBatchGraduationApiConstants.BATCH_DASHBOARD)
            .with(jwt().jwt(jwt -> jwt.claim("scope", "LOAD_STUDENT_IDS")))
            .param("searchParams", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andReturn();
    this.mockMvc.perform(asyncDispatch(result)).andDo(print()).andExpect(status().isOk()).andExpect(MockMvcResultMatchers.jsonPath("$.totalElements", Matchers.equalTo(0)));
  }

  @Test
  void testGetJobHistory_givenInvalidArgument_returnsError() throws Exception {
    final SearchCriteria searchCriteria1 = SearchCriteria.builder()
        .condition(AND)
        .key("id")
        .operation(FilterOperation.EQUAL)
        .value("SUCCESS")
        .valueType(ValueType.UUID)
        .build();

    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(searchCriteria1);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());

    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc
        .perform(get(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_V2_ROOT_MAPPING + EducGradBatchGraduationApiConstants.BATCH_DASHBOARD)
            .with(jwt().jwt(jwt -> jwt.claim("scope", "LOAD_STUDENT_IDS")))
            .param("searchParams", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().is4xxClientError())
        .andExpect(MockMvcResultMatchers.jsonPath("$.messages[0].message").value("Invalid UUID string: SUCCESS"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.messages[0].messageType").value("error"));
  }

  @Test
  void testGetJobHistory_givenNullKey_returnsError() throws Exception {
    final SearchCriteria searchCriteria1 = SearchCriteria.builder()
        .condition(AND)
        .key(null)
        .operation(FilterOperation.EQUAL)
        .value("SUCCESS")
        .valueType(ValueType.STRING)
        .build();

    final List<SearchCriteria> criteriaList = new ArrayList<>();
    criteriaList.add(searchCriteria1);

    final List<Search> searches = new LinkedList<>();
    searches.add(Search.builder().searchCriteriaList(criteriaList).build());

    final String criteriaJSON = objectMapper.writeValueAsString(searches);
    this.mockMvc
        .perform(get(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_V2_ROOT_MAPPING + EducGradBatchGraduationApiConstants.BATCH_DASHBOARD)
            .with(jwt().jwt(jwt -> jwt.claim("scope", "LOAD_STUDENT_IDS")))
            .param("searchParams", criteriaJSON)
            .contentType(APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().is4xxClientError())
        .andExpect(MockMvcResultMatchers.jsonPath("$.messages[0].message").value("Unexpected request parameters provided: Search Criteria can not contain null values for key, value and operation type"))
        .andExpect(MockMvcResultMatchers.jsonPath("$.messages[0].messageType").value("error"));
  }

  private BatchGradAlgorithmJobHistoryEntity createJobHistoryEntity() {
    var entity = new BatchGradAlgorithmJobHistoryEntity();
    entity.setId(UUID.randomUUID());
    entity.setJobExecutionId(123L);
    entity.setStartTime(LocalDateTime.now().minusHours(1));
    entity.setEndTime(LocalDateTime.now());
    entity.setExpectedStudentsProcessed(100L);
    entity.setActualStudentsProcessed(95L);
    entity.setFailedStudentsProcessed(5);
    entity.setStatus("COMPLETED");
    entity.setTriggerBy("SYSTEM");
    entity.setJobType("GRADUATION");
    entity.setLocalDownload("N");
    entity.setJobParameters("param1=value1,param2=value2");
    return entity;
  }
}
