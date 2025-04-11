package ca.bc.gov.educ.api.batchgraduation.controller.v2;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchGradAlgorithmJobHistoryEntity;
import ca.bc.gov.educ.api.batchgraduation.model.*;
import ca.bc.gov.educ.api.batchgraduation.service.BatchGradAlgorithmJobHistorySearchService;
import ca.bc.gov.educ.api.batchgraduation.transformer.BatchGradAlgorithmJobHistoryTransformer;
import ca.bc.gov.educ.api.batchgraduation.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController("jobLauncherControllerV2")
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_V2_ROOT_MAPPING)
@OpenAPIDefinition(info = @Info(title = "API for Manual Triggering of batch process.", description = "This API is for Manual Triggering of batch process.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"LOAD_STUDENT_IDS"})})
public class JobLauncherController {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncherController.class);
    private final BatchGradAlgorithmJobHistorySearchService searchService;
    private final BatchGradAlgorithmJobHistoryTransformer transformer;

    @Autowired
    public JobLauncherController(BatchGradAlgorithmJobHistorySearchService searchService, BatchGradAlgorithmJobHistoryTransformer transformer) {
        this.searchService = searchService;
        this.transformer = transformer;
    }

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_DASHBOARD)
    @PreAuthorize(PermissionsConstants.LOAD_STUDENT_IDS)
    @Operation(summary = "Find all BatchGradAlgorithmJobHistory paginated", description = "Find all batch history using search criteria and pagination", tags = { "Dashboard" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "204", description = "No Content"),
        @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR")})
    public CompletableFuture<Page<BatchGradAlgorithmJobHistory>> loadDashboard(
            @RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "sort", defaultValue = "") String sort,
            @RequestParam(name = "searchParams", required = false) String searchParams) {
        logger.debug("Inside loadDashboard");
        final List<Sort.Order> sorts = new ArrayList<>();
        Specification<BatchGradAlgorithmJobHistoryEntity> spec = searchService.setSpecificationAndSortCriteria(sort, searchParams, new ObjectMapper(), sorts);
        return this.searchService.findAll(spec, pageNumber, pageSize, sorts).thenApplyAsync(data -> data.map(transformer::transformToDTO));
    }
}
