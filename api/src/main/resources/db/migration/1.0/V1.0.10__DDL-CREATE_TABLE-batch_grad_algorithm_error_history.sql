-- API_GRAD_BATCH.BATCH_GRAD_ALGORITHM_ERROR_HISTORY definition

CREATE TABLE "BATCH_GRAD_ALG_ERR_HISTORY"
   (	"BATCH_GRAD_ALG_ERR_HISTORY_ID" RAW(16) DEFAULT SYS_GUID() NOT NULL ENABLE,
	"GRADUATION_STUDENT_RECORD_ID" RAW(16) NOT NULL ENABLE,
	"JOB_EXECUTION_ID" NUMBER(19,0) NOT NULL ENABLE,
	"ERROR" VARCHAR2(250 CHAR),
	"CREATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
	"CREATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
	"UPDATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
	"UPDATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
	 CONSTRAINT "BATCH_GRAD_ALG_ERR_HISTORY_PK" PRIMARY KEY ("BATCH_GRAD_ALG_ERR_HISTORY_ID")
  USING INDEX TABLESPACE "API_GRAD_IDX"  ENABLE,
	 CONSTRAINT "BAT_GRDALG_ERHSTRY_BAT_JBEXEC_FK" FOREIGN KEY ("JOB_EXECUTION_ID")
	  REFERENCES "BATCH_JOB_EXECUTION" ("JOB_EXECUTION_ID") ENABLE
   ) SEGMENT CREATION IMMEDIATE
 NOCOMPRESS LOGGING
  TABLESPACE "API_GRAD_DATA"   NO INMEMORY ;