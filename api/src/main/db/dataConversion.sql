-- Data conversion schema creation
DROP TABLE IF EXISTS "GRADUATION"."CONV_GRAD_STUDENT";
CREATE TABLE "GRADUATION"."CONV_GRAD_STUDENT"
   (
	"PEN" VARCHAR2(9 BYTE) NOT NULL,
	"STUDENT_ID" RAW(16) NOT NULL,
    "FK_GRAD_PROGRAM_CODE" VARCHAR2(8 BYTE),
    "PROGRAM_COMPLETION_DT" DATE,
    "GPA" NUMBER(5,4),
    "HONOURS_STANDING" VARCHAR2(1 BYTE),
    "RECALCULATE_GRAD_STATUS" VARCHAR2(1 BYTE),
    "STUDENT_GRAD_DATA" CLOB,
    "SCHOOL_OF_RECORD" VARCHAR2(9 BYTE),
    "SCHOOL_AT_GRAD" VARCHAR2(9 BYTE),
    "STUD_GRADE" VARCHAR2(3 CHAR),
    "FK_GRAD_STUDENT_STUDENT_STATUS" VARCHAR2(1 BYTE),
	"CREATED_BY" VARCHAR2(20 BYTE) DEFAULT USER,
	"CREATED_TIMESTAMP" TIMESTAMP (6) DEFAULT systimestamp,
	"UPDATED_BY" VARCHAR2(20 BYTE) DEFAULT USER,
	"UPDATED_TIMESTAMP" TIMESTAMP (6) DEFAULT systimestamp
   ) SEGMENT CREATION IMMEDIATE
       PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255
      NOCOMPRESS LOGGING
       STORAGE(INITIAL 131072 NEXT 131072 MINEXTENTS 1 MAXEXTENTS 2147483645
       PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
       BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
       TABLESPACE "GRADUATION_DATA"   NO INMEMORY
      LOB ("STUDENT_GRAD_DATA") STORE AS SECUREFILE (
       TABLESPACE "GRADUATION_DATA" ENABLE STORAGE IN ROW CHUNK 8192
       NOCACHE LOGGING  NOCOMPRESS  KEEP_DUPLICATES
       STORAGE(INITIAL 131072 NEXT 131072 MINEXTENTS 1 MAXEXTENTS 2147483645
       PCTINCREASE 0
       BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)) ;

 --------------------------------------------------------
 --  Constraints for Table GRAD_STUDENT
 --------------------------------------------------------

   ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT" MODIFY ("FK_GRAD_PROGRAM_CODE" NOT NULL ENABLE);
   ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT" MODIFY ("STUDENT_ID" NOT NULL ENABLE);
   ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT" ADD CONSTRAINT "CONV_GRAD_STUDENT_PK" PRIMARY KEY ("STUDENT_ID")
   USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
   STORAGE(INITIAL 131072 NEXT 131072 MINEXTENTS 1 MAXEXTENTS 2147483645
   PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
   BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
   TABLESPACE "GRADUATION_DATA"  ENABLE;
   ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT" MODIFY ("PEN" NOT NULL ENABLE);
   ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT" MODIFY ("CREATED_BY" NOT NULL ENABLE);
   ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT" MODIFY ("CREATED_TIMESTAMP" NOT NULL ENABLE);
   ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT" MODIFY ("UPDATED_BY" NOT NULL ENABLE);
   ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT" MODIFY ("UPDATED_TIMESTAMP" NOT NULL ENABLE);

--------------------------------------------------------
--  DDL for Table GRAD_STUDENT_SPECIAL_PROGRAMS
--------------------------------------------------------

CREATE TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS"
(	"ID" RAW(16) DEFAULT SYS_GUID(),
     "FK_GRAD_STUDENT_PEN" VARCHAR2(9 BYTE),
     "FK_GRAD_SPECIAL_PROGRAM_ID" RAW(16),
     "SPECIAL_PROGRAM_COMP_DT" DATE,
     "STUDENT_SPECIAL_PROGRAM_DATA" CLOB,
     "CREATED_BY" VARCHAR2(20 BYTE) DEFAULT USER,
     "CREATED_TIMESTAMP" TIMESTAMP (6) DEFAULT systimestamp,
     "UPDATED_BY" VARCHAR2(20 BYTE) DEFAULT USER,
     "UPDATED_TIMESTAMP" TIMESTAMP (6) DEFAULT systimestamp,
     "FK_GRAD_STUDENT_STUDENT_ID" RAW(16)
) SEGMENT CREATION IMMEDIATE
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255
 NOCOMPRESS LOGGING
  STORAGE(INITIAL 131072 NEXT 131072 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "GRADUATION_DATA"   NO INMEMORY
 LOB ("STUDENT_SPECIAL_PROGRAM_DATA") STORE AS SECUREFILE (
  TABLESPACE "GRADUATION_DATA" ENABLE STORAGE IN ROW CHUNK 8192
  NOCACHE LOGGING  NOCOMPRESS  KEEP_DUPLICATES
  STORAGE(INITIAL 131072 NEXT 131072 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)) ;

--------------------------------------------------------
--  Constraints for Table GRAD_STUDENT_SPECIAL_PROGRAMS
--------------------------------------------------------

ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" MODIFY ("ID" NOT NULL ENABLE);
ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" ADD CONSTRAINT "CONV_GRAD_STUDENT_SPECIAL_PROGRAMS_PK" PRIMARY KEY ("ID")
    USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS
  STORAGE(INITIAL 131072 NEXT 131072 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1
  BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "GRADUATION_DATA"  ENABLE;
ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" MODIFY ("FK_GRAD_STUDENT_PEN" NOT NULL ENABLE);
ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" MODIFY ("FK_GRAD_SPECIAL_PROGRAM_ID" NOT NULL ENABLE);
ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" MODIFY ("CREATED_BY" NOT NULL ENABLE);
ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" MODIFY ("CREATED_TIMESTAMP" NOT NULL ENABLE);
ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" MODIFY ("UPDATED_BY" NOT NULL ENABLE);
ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" MODIFY ("UPDATED_TIMESTAMP" NOT NULL ENABLE);
--------------------------------------------------------
--  Ref Constraints for Table GRAD_STUDENT_SPECIAL_PROGRAMS
--------------------------------------------------------

ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" ADD CONSTRAINT "CONV_GRAD_STUDENT_SPECIAL_PROGRAMS_FK1" FOREIGN KEY ("FK_GRAD_STUDENT_STUDENT_ID")
    REFERENCES "GRADUATION"."CONV_GRAD_STUDENT" ("STUDENT_ID") ENABLE;
ALTER TABLE "GRADUATION"."CONV_GRAD_STUDENT_SPECIAL_PROGRAMS" ADD CONSTRAINT "CONV_GRAD_STUDENT_SPECIAL_PROGRAMS_FK2" FOREIGN KEY ("FK_GRAD_SPECIAL_PROGRAM_ID")
    REFERENCES "GRADUATION"."GRAD_SPECIAL_PROGRAM" ("ID") ENABLE;

------------------------------
-- Initial data set from TRAX
------------------------------
-- GRAD_STUDENT
select  trim(m.stud_no) as PEN, m.mincode as SCHOOL_OF_RECORD, m.mincode_grad as SCHOOL_AT_GRAD, m.stud_grade as STUD_GRADE, m.stud_status as STUD_STATUS, '2018-EN', 'Y'
from trax_students_load l, student_master m
where 1 = 1
and l.stud_no = m.stud_no
and m.grad_date = 0
and m.archive_flag = 'A'

-- GRAD_COURSE_RESTRICTIONS
select trim(c1.crse_code) as CRSE_MAIN, trim(c1.crse_level) as CRSE_MAIN_LVL,
 trim(c2.crse_code) as CRSE_RESTRICTED, trim(c2.crse_level) as CRSE_RESTRICTED_LVL,
 trim(c1.start_restrict_session) as RESTRICTION_START_DT, trim(c1.end_restrict_session) as RESTRICTION_END_DT
from tab_crse c1
join tab_crse c2
on c1.restriction_code = c2.restriction_code
and (c1.crse_code  <> c2.crse_code
 or  c1.crse_level <> c2.crse_level)
and c1.restriction_code <> ' '

------------------------------
-- Validation Queries
------------------------------
-- French immersion validation by pen
select count(*) from STUD_XCRSE sx, GRAD_COURSE_REQUIREMENT gcr
where 1 = 1
  and sx.stud_no = '131493553'  -- pen
  and trim(sx.crse_code) = gcr.crse_code
  and trim(sx.crse_level) = gcr.crse_lvl
  and gcr.rule_code = 202
