package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.exception.InvalidParameterException;
import ca.bc.gov.educ.api.batchgraduation.exception.ServiceException;
import ca.bc.gov.educ.api.batchgraduation.filter.BatchGradAlgJobHistoryFilterSpecifics;
import ca.bc.gov.educ.api.batchgraduation.filter.FilterOperation;
import ca.bc.gov.educ.api.batchgraduation.model.dto.Search;
import ca.bc.gov.educ.api.batchgraduation.model.dto.SearchCriteria;
import ca.bc.gov.educ.api.batchgraduation.model.dto.ValueType;
import ca.bc.gov.educ.api.batchgraduation.model.dto.Condition;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchGradAlgorithmJobHistoryRepository;
import ca.bc.gov.educ.api.batchgraduation.util.RequestUtil;
import ca.bc.gov.educ.api.batchgraduation.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class BatchGradAlgorithmJobHistorySearchService {
  private final BatchGradAlgJobHistoryFilterSpecifics filterSpecs;
  private final BatchGradAlgorithmJobHistoryRepository repository;

  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
      .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  public BatchGradAlgorithmJobHistorySearchService(BatchGradAlgJobHistoryFilterSpecifics filterSpecs, BatchGradAlgorithmJobHistoryRepository repository) {
    this.filterSpecs = filterSpecs;
    this.repository = repository;
  }

  public Specification<BatchGradAlgorithmJobHistoryEntity> getSpecifications(Specification<BatchGradAlgorithmJobHistoryEntity> schoolSpecs, int i, Search search) {
    if (i == 0) {
      schoolSpecs = getSpecification(search.getSearchCriteriaList());
    } else {
      if (search.getCondition() == Condition.AND) {
        schoolSpecs = schoolSpecs.and(getSpecification(search.getSearchCriteriaList()));
      } else {
        schoolSpecs = schoolSpecs.or(getSpecification(search.getSearchCriteriaList()));
      }
    }
    return schoolSpecs;
  }

  private Specification<BatchGradAlgorithmJobHistoryEntity> getSpecification(List<SearchCriteria> criteriaList) {
    Specification<BatchGradAlgorithmJobHistoryEntity> schoolSpecs = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          var criteriaValue = criteria.getValue();
          if(StringUtils.isNotBlank(criteria.getValue()) && TransformUtil.isUppercaseField(BatchGradAlgorithmJobHistoryEntity.class, criteria.getKey())) {
            criteriaValue = criteriaValue.toUpperCase();
          }
          Specification<BatchGradAlgorithmJobHistoryEntity> typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteriaValue, criteria.getValueType());
          schoolSpecs = getSpecificationPerGroup(schoolSpecs, i, criteria, typeSpecification);
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for key, value and operation type");
        }
      }
    }
    return schoolSpecs;
  }
  private Specification<BatchGradAlgorithmJobHistoryEntity> getSpecificationPerGroup(Specification<BatchGradAlgorithmJobHistoryEntity> specification, int i, SearchCriteria criteria, Specification<BatchGradAlgorithmJobHistoryEntity> typeSpecification) {
    if (i == 0) {
      specification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        specification = specification.and(typeSpecification);
      } else {
        specification = specification.or(typeSpecification);
      }
    }
    return specification;
  }

  private Specification<BatchGradAlgorithmJobHistoryEntity> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType) {
    Specification<BatchGradAlgorithmJobHistoryEntity> specification = null;
    switch (valueType) {
      case STRING:
        specification = filterSpecs.getStringTypeSpecification(key, value, filterOperation);
        break;
      case DATE_TIME:
        specification = filterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
        break;
      case LONG:
        specification = filterSpecs.getLongTypeSpecification(key, value, filterOperation);
        break;
      case INTEGER:
        specification = filterSpecs.getIntegerTypeSpecification(key, value, filterOperation);
        break;
      case DATE:
        specification = filterSpecs.getDateTypeSpecification(key, value, filterOperation);
        break;
      case UUID:
        specification = filterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
        break;
      default:
        break;
    }
    return specification;
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<BatchGradAlgorithmJobHistoryEntity>> findAll(Specification<BatchGradAlgorithmJobHistoryEntity> schoolSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    log.trace("In find all query: {}", schoolSpecs);
    return CompletableFuture.supplyAsync(() -> {
      Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        log.trace("Running paginated query: {}", schoolSpecs);
        var results = this.repository.findAll(schoolSpecs, paging);
        log.trace("Paginated query returned with results: {}", results);
        return results;
      } catch (final Throwable ex) {
        log.error("Failure querying for paginated schools: {}", ex.getMessage());
        throw new CompletionException(ex);
      }
    }, paginatedQueryExecutor);

  }

  public Specification<BatchGradAlgorithmJobHistoryEntity> setSpecificationAndSortCriteria(String sortCriteriaJson, String searchCriteriaListJson, ObjectMapper objectMapper, List<Sort.Order> sorts) {
    Specification<BatchGradAlgorithmJobHistoryEntity> schoolSpecs = null;
    try {
      RequestUtil.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        int i = 0;
        for (var search : searches) {
          schoolSpecs = getSpecifications(schoolSpecs, i, search);
          i++;
        }
      }
    } catch (JsonProcessingException e) {
      throw new ServiceException(e.getMessage());
    }
    return schoolSpecs;
  }
}
