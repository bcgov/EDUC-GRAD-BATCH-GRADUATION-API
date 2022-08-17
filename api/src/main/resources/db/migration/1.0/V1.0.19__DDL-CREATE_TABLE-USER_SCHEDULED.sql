-- API_GRAD_DATA_CONV.USER_SCHEDULED_JOBS definition

CREATE TABLE "USER_SCHEDULED_JOBS"
   (	"USER_SCHEDULED_JOBS_ID" RAW(16) DEFAULT SYS_GUID() NOT NULL ENABLE,
	"JOB_CODE" VARCHAR2(15),
	"CRON_EXPRESSION" VARCHAR2(24),
	"JOB_NAME" VARCHAR2(250),
	"JOB_PARAMS" CLOB,
	"STATUS" VARCHAR2(20),
	"CREATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
    "CREATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
    "UPDATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
    "UPDATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
	 CONSTRAINT "USER_SCHEDULED_JOBS_PK" PRIMARY KEY ("USER_SCHEDULED_JOBS_ID")
  USING INDEX TABLESPACE "API_GRAD_IDX"  ENABLE
   ) SEGMENT CREATION IMMEDIATE
 NOCOMPRESS LOGGING
  TABLESPACE "API_GRAD_DATA"   NO INMEMORY ;

COMMENT ON TABLE USER_SCHEDULED_JOBS IS 'This table is used to achieve store user scheduled jobs.';