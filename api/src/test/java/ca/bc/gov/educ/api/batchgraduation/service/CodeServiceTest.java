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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@SuppressWarnings({"rawtypes"})
class CodeServiceTest {

	@Autowired
	private CodeService codeService;

	@MockBean
	private BatchJobTypeRepository batchJobTypeRepository;

	@MockBean
	WebClient webClient;

	@Autowired
	GradValidation validation;
	
	@Test
	void testGetAllBatchJobTypesCodeList() {
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
		var result = codeService.getAllBatchJobTypeCodeList();
		assertThat(result).isNotNull().isNotEmpty();
	}
	
	@Test
	void testGetSpecificBatchJobTypeCode() {
		String code = "TVRRUN";
		BatchJobType obj = new BatchJobType();
		obj.setCode("TVRRUN");
		obj.setDescription("Student Achievement Report (TVR)");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(LocalDateTime.now());
		obj.setUpdateDate(LocalDateTime.now());
		obj.toString();
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("TVRRUN");
		objEntity.setDescription("Student Achievement Report (TVR)");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Optional<BatchJobTypeEntity> ent = Optional.of(objEntity);
		Mockito.when(batchJobTypeRepository.findById(code)).thenReturn(ent);
		var result = codeService.getSpecificBatchJobTypeCode(code);
		assertThat(result).isNotNull();
		assertThat(result.getLabel()).isNotNull();
	}
	
	@Test
	void testGetSpecificBatchJobTypeCodeReturnsNull() {
		String code = "TVRRUN";
		Mockito.when(batchJobTypeRepository.findById(code)).thenReturn(Optional.empty());
		var result = codeService.getSpecificBatchJobTypeCode(code);
		assertThat(result).isNull();
	}
	
	@Test
	void testCreateBatchJobType() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("PSIRUN");
		obj.setDescription("PSI Run FTP / Paper");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(LocalDateTime.now());
		obj.setUpdateDate(LocalDateTime.now());
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("PSIRUN");
		objEntity.setDescription("PSI Run FTP / Paper");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Mockito.when(batchJobTypeRepository.findById(obj.getCode())).thenReturn(Optional.empty());
		Mockito.when(batchJobTypeRepository.save(objEntity)).thenReturn(objEntity);
		var result = codeService.createBatchJobType(obj);
		assertThat(result).isNotNull();
		
	}
	
	@Test(expected = GradBusinessRuleException.class)
	void testCreateBatchJobType_codeAlreadyExists() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("PSIRUN");
		obj.setDescription("PSI Run FTP / Paper");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(LocalDateTime.now());
		obj.setUpdateDate(LocalDateTime.now());
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("PSIRUN");
		objEntity.setDescription("PSI Run FTP / Paper");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Optional<BatchJobTypeEntity> ent = Optional.of(objEntity);
		Mockito.when(batchJobTypeRepository.findById(obj.getCode())).thenReturn(ent);
		var result = codeService.createBatchJobType(obj);
		assertThat(result).isNotNull();
		
	}
	
	@Test
	void testUpdateBatchJobType() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("REGALG");
		obj.setDescription("Graduation Algorithm");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(LocalDateTime.now());
		obj.setUpdateDate(LocalDateTime.now());
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
		var result = codeService.updateBatchJobType(obj);
		assertThat(result).isNotNull();
	}
	
	@Test(expected = GradBusinessRuleException.class)
	void testUpdateBatchJobType_codeAlreadyExists() {
		BatchJobType obj = new BatchJobType();
		obj.setCode("REGALG");
		obj.setDescription("Graduation Algorithm");
		obj.setCreateUser("GRADUATION");
		obj.setUpdateUser("GRADUATION");
		obj.setCreateDate(LocalDateTime.now());
		obj.setUpdateDate(LocalDateTime.now());
		BatchJobTypeEntity objEntity = new BatchJobTypeEntity();
		objEntity.setCode("REGALG");
		objEntity.setDescription("Graduation Algorithm");
		objEntity.setCreateUser("GRADUATION");
		objEntity.setUpdateUser("GRADUATION");
		objEntity.setCreateDate(new Date(System.currentTimeMillis()));
		objEntity.setUpdateDate(new Date(System.currentTimeMillis()));
		Mockito.when(batchJobTypeRepository.findById(obj.getCode())).thenReturn(Optional.empty());
		var result = codeService.updateBatchJobType(obj);
		assertThat(result).isNull();
	}

}
