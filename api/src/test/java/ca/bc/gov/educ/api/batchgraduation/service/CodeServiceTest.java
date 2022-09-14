package ca.bc.gov.educ.api.batchgraduation.service;

import ca.bc.gov.educ.api.batchgraduation.entity.BatchJobTypeEntity;
import ca.bc.gov.educ.api.batchgraduation.exception.GradBusinessRuleException;
import ca.bc.gov.educ.api.batchgraduation.model.BatchJobType;
import ca.bc.gov.educ.api.batchgraduation.repository.BatchJobTypeRepository;
import ca.bc.gov.educ.api.batchgraduation.util.GradValidation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes"})
public class CodeServiceTest {

	@Autowired
	private CodeService codeService;

	@MockBean
	private BatchJobTypeRepository batchJobTypeRepository;

	@MockBean
	WebClient webClient;

	@Autowired
	GradValidation validation;
	
	@Test
	public void testGetAllBatchJobTypesCodeList() {
		List<BatchJobTypeEntity> gradBatchJobTypeList = new ArrayList<>();
		BatchJobTypeEntity obj = new BatchJobTypeEntity();
		obj.setCode("REGALG");
		obj.setDescription("Graduation Algorithm");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		gradBatchJobTypeList.add(obj);
		obj = new BatchJobTypeEntity();
		obj.setCode("TVRRUN");
		obj.setDescription("Student Achievement Report (TVR)");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		gradBatchJobTypeList.add(obj);
		Mockito.when(batchJobTypeRepository.findAll()).thenReturn(gradBatchJobTypeList);
		codeService.getAllBatchJobTypeCodeList();
	}
	
	@Test
	public void testGetSpecificBatchJobTypeCode() {
		String certCode = "E";
		BatchJobType obj = new BatchJobType();
		obj.setCode("TVRRUN");
		obj.setDescription("Student Achievement Report (TVR)");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		obj.toString();
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("TVRRUN");
		objEntity.setDescription("Student Achievement Report (TVR)");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Optional<BatchJobTypeEntity> ent = Optional.of(objEntity);
		Mockito.when(batchJobTypeRepository.findById(certCode)).thenReturn(ent);
		codeService.getSpecificBatchJobTypeCode(certCode);
	}
	
	@Test
	public void testGetSpecificBatchJobTypeCodeReturnsNull() {
		String certCode = "TVRRUN";
		Mockito.when(batchJobTypeRepository.findById(certCode)).thenReturn(Optional.empty());
		codeService.getSpecificBatchJobTypeCode(certCode);
	}
	
	@Test
	public void testCreateBatchJobType() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("PSIRUN");
		obj.setDescription("PSI Run FTP / Paper");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("PSIRUN");
		objEntity.setDescription("PSI Run FTP / Paper");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Mockito.when(batchJobTypeRepository.findById(obj.getCode())).thenReturn(Optional.empty());
		Mockito.when(batchJobTypeRepository.save(objEntity)).thenReturn(objEntity);
		codeService.createBatchJobType(obj);
		
	}
	
	@Test(expected = GradBusinessRuleException.class)
	public void testCreateBatchJobType_codeAlreadyExists() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("PSIRUN");
		obj.setDescription("PSI Run FTP / Paper");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("PSIRUN");
		objEntity.setDescription("PSI Run FTP / Paper");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Optional<BatchJobTypeEntity> ent = Optional.of(objEntity);
		Mockito.when(batchJobTypeRepository.findById(obj.getCode())).thenReturn(ent);
		codeService.createBatchJobType(obj);
		
	}
	
	@Test
	public void testUpdateBatchJobType() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("REGALG");
		obj.setDescription("Graduation Algorithm");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("REGALG");
		objEntity.setDescription("Graduation Algorithm");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Optional<BatchJobTypeEntity> ent = Optional.of(objEntity);
		Mockito.when(batchJobTypeRepository.findById(obj.getCode())).thenReturn(ent);
		Mockito.when(batchJobTypeRepository.save(objEntity)).thenReturn(objEntity);
		codeService.updateBatchJobType(obj);		
	}
	
	@Test(expected = GradBusinessRuleException.class)
	public void testUpdateBatchJobType_codeAlreadyExists() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("REGALG");
		obj.setDescription("Graduation Algorithm");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(new Date(System.currentTimeMillis()));
		obj.setUpdateDate(new Date(System.currentTimeMillis()));
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("REGALG");
		objEntity.setDescription("Graduation Algorithm");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Mockito.when(batchJobTypeRepository.findById(obj.getCode())).thenReturn(Optional.empty());
		codeService.updateBatchJobType(obj);
		
	}

}
