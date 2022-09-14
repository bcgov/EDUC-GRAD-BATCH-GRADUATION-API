package ca.bc.gov.educ.api.batchgraduation.controller;

import ca.bc.gov.educ.api.batchgraduation.model.BatchJobType;
import ca.bc.gov.educ.api.batchgraduation.service.CodeService;
import ca.bc.gov.educ.api.batchgraduation.util.GradValidation;
import ca.bc.gov.educ.api.batchgraduation.util.MessageHelper;
import ca.bc.gov.educ.api.batchgraduation.util.ResponseHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


@ExtendWith(MockitoExtension.class)
class CodeControllerTest {

	@Mock
	private CodeService codeService;
	
	@Mock
	ResponseHelper response;
	
	@InjectMocks
	private CodeController codeController;
	
	@Mock
	GradValidation validation;
	
	@Mock
	MessageHelper messagesHelper;
	
	@Test
	void testGetAllBatchJobTypeCodeList() {
		List<BatchJobType> batchJobTypes = new ArrayList<>();
		BatchJobType obj = new BatchJobType();
		obj.setCode("DC");
		obj.setDescription("Data Correction by School");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		batchJobTypes.add(obj);
		obj = new BatchJobType();
		obj.setCode("CC");
		obj.setDescription("Courses not complete");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		batchJobTypes.add(obj);
		Mockito.when(codeService.getAllBatchJobTypeCodeList()).thenReturn(batchJobTypes);
		codeController.getAllBatchJobTypeCodeList();
		Mockito.verify(codeService).getAllBatchJobTypeCodeList();

	}
	
	@Test
	void testGetSpecificBatchJobTypeCode() {
		String batchJobType = "DC";
		BatchJobType obj = new BatchJobType();
		obj.setCode("DC");
		obj.setDescription("Data Correction by School");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		Mockito.when(codeService.getSpecificBatchJobTypeCode(batchJobType)).thenReturn(obj);
		codeController.getSpecificBatchJobTypeCode(batchJobType);
		Mockito.verify(codeService).getSpecificBatchJobTypeCode(batchJobType);
	}
	
	@Test
	void testGetSpecificBatchJobTypeCode_noContent() {
		String batchJobType = "AB";
		Mockito.when(codeService.getSpecificBatchJobTypeCode(batchJobType)).thenReturn(null);
		codeController.getSpecificBatchJobTypeCode(batchJobType);
		Mockito.verify(codeService).getSpecificBatchJobTypeCode(batchJobType);
	}
	
	@Test
	void testCreateBatchJobTypeCode() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("DC");
		obj.setDescription("Data Correction by School");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		Mockito.when(codeService.createBatchJobType(obj)).thenReturn(obj);
		codeController.createBatchJobType(obj);
		Mockito.verify(codeService).createBatchJobType(obj);
	}
	
	@Test
	void testUpdateBatchJobTypeCode() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("DC");
		obj.setDescription("Data Correction by School");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		Mockito.when(codeService.updateBatchJobType(obj)).thenReturn(obj);
		codeController.updateBatchJobType(obj);
		Mockito.verify(codeService).updateBatchJobType(obj);
	}
	
	@Test
	void testDeleteBatchJobTypeCode() {
		String statusCode = "DC";
		Mockito.when(codeService.deleteBatchJobType(statusCode)).thenReturn(1);
		codeController.deleteBatchJobType(statusCode);
		Mockito.verify(codeService).deleteBatchJobType(statusCode);
	}
}
