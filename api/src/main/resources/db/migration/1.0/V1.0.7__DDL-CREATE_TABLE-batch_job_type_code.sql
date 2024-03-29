-- API_GRAD_BATCH.BATCH_JOB_TYPE_CODE definition

CREATE TABLE "BATCH_JOB_TYPE_CODE"
   (	"BATCH_JOB_TYPE_CODE" VARCHAR2(15),
	"LABEL" VARCHAR2(32) NOT NULL ENABLE,
	"DESCRIPTION" VARCHAR2(255) NOT NULL ENABLE,
	"DISPLAY_ORDER" NUMBER NOT NULL ENABLE,
	"EFFECTIVE_DATE" DATE NOT NULL ENABLE,
	"EXPIRY_DATE" DATE,
	"CREATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
	"CREATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
	"UPDATE_USER" VARCHAR2(32) DEFAULT USER NOT NULL ENABLE,
	"UPDATE_DATE" DATE DEFAULT SYSTIMESTAMP NOT NULL ENABLE,
	 CONSTRAINT "BATCH_JOB_TYPE_CODE_PK" PRIMARY KEY ("BATCH_JOB_TYPE_CODE")
  USING INDEX TABLESPACE "API_GRAD_IDX"  ENABLE
   ) SEGMENT CREATION IMMEDIATE
 NOCOMPRESS LOGGING
  TABLESPACE "API_GRAD_DATA"   NO INMEMORY ;
