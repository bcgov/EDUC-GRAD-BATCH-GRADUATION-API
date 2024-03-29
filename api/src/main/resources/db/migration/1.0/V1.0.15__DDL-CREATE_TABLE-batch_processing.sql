-- API_GRAD_DATA_CONV.BATCH_PROCESSING definition

CREATE TABLE "BATCH_PROCESSING"
   (	"BATCH_PROCESSING_ID" RAW(16) DEFAULT SYS_GUID() NOT NULL ENABLE,
	"BATCH_JOB_TYPE_CODE" VARCHAR2(15),
	"CRON_EXPRESSION" VARCHAR2(24),
	"SCHEDULE_OCCURRENCE" VARCHAR2(1),
	"ENABLED" VARCHAR2(1),
	"CREATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
    "CREATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
    "UPDATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
    "UPDATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
	 CONSTRAINT "BATCH_PROCESSING_PK" PRIMARY KEY ("BATCH_PROCESSING_ID")
  USING INDEX TABLESPACE "API_GRAD_IDX"  ENABLE,
    CONSTRAINT "BATCH_PROCESSING_BJTC_CODE_FK" FOREIGN KEY ("BATCH_JOB_TYPE_CODE")
    	  REFERENCES "BATCH_JOB_TYPE_CODE" ("BATCH_JOB_TYPE_CODE") ENABLE
   ) SEGMENT CREATION IMMEDIATE
 NOCOMPRESS LOGGING
  TABLESPACE "API_GRAD_DATA"   NO INMEMORY ;

COMMENT ON TABLE BATCH_PROCESSING IS 'This table is used to achieve dynamic batch scheduling.';