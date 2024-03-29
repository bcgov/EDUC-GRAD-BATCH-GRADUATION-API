CREATE OR ALTER PROCEDURE [initialDataLoad]

BEGIN
CREATE TABLE "GRADUATION"."GRAD_ALGORITHM_RULES_TEMP" 
   (	
   	"ID" RAW(16) DEFAULT SYS_GUID () NOT NULL ENABLE, 
	"RULE_NAME" VARCHAR2(150 CHAR), 
	"RULE_IMPLEMENTATION" VARCHAR2(150 CHAR), 
	"RULE_DESCRIPTION" VARCHAR2(150 CHAR), 
	"SORT_ORDER" NUMBER(5,0), 
	"FK_GRAD_PROGRAM_CODE" VARCHAR2(8 BYTE), 
	"CREATED_BY" VARCHAR2(20 BYTE) DEFAULT USER NOT NULL ENABLE, 
	"CREATED_TIMESTAMP" TIMESTAMP (6) DEFAULT systimestamp NOT NULL ENABLE, 
	"UPDATED_BY" VARCHAR2(20 BYTE) DEFAULT USER NOT NULL ENABLE, 
	"UPDATED_TIMESTAMP" TIMESTAMP (6) DEFAULT systimestamp NOT NULL ENABLE, 
	"SPECIAL_PROGRAM" VARCHAR2(8 BYTE), 
	"IS_ACTIVE" VARCHAR2(1 BYTE) DEFAULT NULL
   )
Insert into GRADUATION.GRAD_ALGORITHM_RULES_TEMP (ID,RULE_NAME,RULE_IMPLEMENTATION,RULE_DESCRIPTION,SORT_ORDER,FK_GRAD_PROGRAM_CODE,CREATED_BY,CREATED_TIMESTAMP,UPDATED_BY,UPDATED_TIMESTAMP,SPECIAL_PROGRAM,IS_ACTIVE) values ('BE75BC4E6FA866A5E0539AE9228EEB4D','MatchCredits','MatchCreditsRule','Student must meet a minimum of 4-credits from each of the specified non-elective subject areas',90,'2018-EN','GRADUATION',to_timestamp('26-MAR-21 04.30.11.406203000 PM','DD-MON-RR HH.MI.SSXFF AM'),'GRADUATION',to_timestamp('26-MAR-21 04.30.11.406203000 PM','DD-MON-RR HH.MI.SSXFF AM'),null,'Y');	
END;
