-- API_GRAD_BATCH.BATCH_GRAD_ALGORITHM_JOB_HISTORY definition

CREATE TABLE "BATCH_GRAD_ALGORITHM_JOB_HISTORY"
   (	"JOB_EXECUTION_ID" NUMBER(19,0) NOT NULL ENABLE,
	"START_TIME" TIMESTAMP (6) DEFAULT NULL,
	"END_TIME" TIMESTAMP (6) DEFAULT NULL,
	"EXPECTED_STUDENTS_PROCESSED" NUMBER(6,0),
	"ACTUAL_STUDENTS_PROCESSED" NUMBER(6,0),
	"STATUS" VARCHAR2(10 CHAR),
	"CREATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
	"CREATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
	"UPDATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
	"UPDATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
	"FAILED_STUDENTS_PROCESSED" NUMBER(6,0),
	"BATCH_GRAD_ALGORITHM_JOB_HISTORY_ID" RAW(16) DEFAULT SYS_GUID() NOT NULL ENABLE,
	"BATCH_JOB_TRIGGER_CODE" VARCHAR2(10),
	 CONSTRAINT "BATCH_GRAD_ALGORITHM_JOB_HISTORY_PK" PRIMARY KEY ("BATCH_GRAD_ALGORITHM_JOB_HISTORY_ID")
  USING INDEX TABLESPACE "API_GRAD_IDX"  ENABLE,
	 CONSTRAINT "BAT_GRDALGO_JBHSTRY_BAT_JBEXEC_FK" FOREIGN KEY ("JOB_EXECUTION_ID")
	  REFERENCES "BATCH_JOB_EXECUTION" ("JOB_EXECUTION_ID") ENABLE,
	 CONSTRAINT "BAT_GRDALGO_JBHSTRY_BAT_JBTRGCD_FK" FOREIGN KEY ("BATCH_JOB_TRIGGER_CODE")
	  REFERENCES "BATCH_JOB_TRIGGER_CODE" ("BATCH_JOB_TRIGGER_CODE") ENABLE
   ) SEGMENT CREATION IMMEDIATE
 NOCOMPRESS LOGGING
  TABLESPACE "API_GRAD_DATA"   NO INMEMORY ;
