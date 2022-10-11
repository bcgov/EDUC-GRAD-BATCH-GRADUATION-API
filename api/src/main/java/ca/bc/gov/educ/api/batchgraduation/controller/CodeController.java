package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.BatchJobType;
import ca.bc.gov.educ.api.batchgraduation.service.CodeService;
import ca.bc.gov.educ.api.batchgraduation.util.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(EducGradBatchGraduationApiConstants.GRAD_BATCH_API_ROOT_MAPPING)
@CrossOrigin
@OpenAPIDefinition(info = @Info(title = "API for Code Tables Data.",
        description = "This API is for Reading Code Tables data.", version = "1"),
        security = {@SecurityRequirement(name = "OAUTH2",
                scopes = {"READ_GRAD_BATCH_JOB_CODE_DATA",
                        "DELETE_GRAD_BATCH_JOB_CODE_DATA",
                        "UPDATE_GRAD_BATCH_JOB_CODE_DATA",
                        "CREATE_GRAD_BATCH_JOB_CODE_DATA"}
        )
})
public class CodeController {

    private static Logger logger = LoggerFactory.getLogger(CodeController.class);
    
    private static final String BATCH_JOB_TYPE_CODE ="Batch Job Type Code";

    @Autowired
    CodeService codeService;

    @Autowired
    GradValidation validation;

    @Autowired
    ResponseHelper response;

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_JOB_TYPES_MAPPING)
    @PreAuthorize(PermissionsConstants.READ_BATCH_JOB_TYPE)
    @Operation(summary = "Find all Batch Job Types", description = "Get all Batch Job Types", tags = {"Batch Job"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<BatchJobType>> getAllBatchJobTypeCodeList() {
        logger.debug("getAllBatchJobTypeCodeList : ");
        return response.GET(codeService.getAllBatchJobTypeCodeList());
    }

    @GetMapping(EducGradBatchGraduationApiConstants.BATCH_JOB_TYPE_MAPPING)
    @PreAuthorize(PermissionsConstants.READ_BATCH_JOB_TYPE)
    @Operation(summary = "Find a Batch Job Type by code", description = "Get a Batch Job Type by Code", tags = {"Batch Job"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "204", description = "NO CONTENT")})
    public ResponseEntity<BatchJobType> getSpecificBatchJobTypeCode(@PathVariable String batchJobTypeCode) {
        logger.debug("getSpecificBatchJobTypeCode : ");
        BatchJobType gradResponse = codeService.getSpecificBatchJobTypeCode(batchJobTypeCode);
        if (gradResponse != null) {
            return response.GET(gradResponse);
        } else {
            return response.NOT_FOUND();
        }
    }

    @PostMapping(EducGradBatchGraduationApiConstants.BATCH_JOB_TYPES_MAPPING)
    @PreAuthorize(PermissionsConstants.CREATE_BATCH_JOB_TYPE)
    @Operation(summary = "Create a Batch Job Type", description = "Create a Batch Job Type", tags = {"Batch Job"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    public ResponseEntity<ApiResponseModel<BatchJobType>> createBatchJobType(
            @Valid @RequestBody BatchJobType batchJobType) {
        logger.debug("createBatchJobType : ");
        validation.clear();
        validation.requiredField(batchJobType.getCode(), BATCH_JOB_TYPE_CODE);
        validation.requiredField(batchJobType.getDescription(), "Batch Job Type Description");
        if (validation.hasErrors()) {
            validation.stopOnErrors();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response.CREATED(codeService.createBatchJobType(batchJobType));
    }

    @PutMapping(EducGradBatchGraduationApiConstants.BATCH_JOB_TYPES_MAPPING)
    @PreAuthorize(PermissionsConstants.UPDATE_BATCH_JOB_TYPE)
    @Operation(summary = "Update a Batch Job Type", description = "Update a Batch Job Type", tags = {"Batch Job"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    public ResponseEntity<ApiResponseModel<BatchJobType>> updateBatchJobType(
            @Valid @RequestBody BatchJobType batchJobType) {
        logger.info("updateBatchJobType : ");
        validation.clear();
        validation.requiredField(batchJobType.getCode(), BATCH_JOB_TYPE_CODE);
        validation.requiredField(batchJobType.getDescription(), "Batch Job Type Description");
        if (validation.hasErrors()) {
            validation.stopOnErrors();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response.UPDATED(codeService.updateBatchJobType(batchJobType));
    }

    @DeleteMapping(EducGradBatchGraduationApiConstants.BATCH_JOB_TYPE_MAPPING)
    @PreAuthorize(PermissionsConstants.DELETE_BATCH_JOB_TYPE)
    @Operation(summary = "Delete a Batch Job Type", description = "Delete a Batch Job", tags = {"Batch Job"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    public ResponseEntity<Void> deleteBatchJobType(@Valid @PathVariable String batchJobTypeCode) {
        logger.debug("deleteBatchJobType : ");
        validation.clear();
        validation.requiredField(batchJobTypeCode, BATCH_JOB_TYPE_CODE);
        if (validation.hasErrors()) {
            validation.stopOnErrors();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return response.DELETE(codeService.deleteBatchJobType(batchJobTypeCode));
    }
}
