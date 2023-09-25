UPDATE BATCH_JOB_TYPE_CODE
SET DISPLAY_ORDER=90,
WHERE BATCH_JOB_TYPE_CODE='CERT_REGEN';

INSERT INTO BATCH_JOB_TYPE_CODE (BATCH_JOB_TYPE_CODE,LABEL,DESCRIPTION,DISPLAY_ORDER,EFFECTIVE_DATE)
VALUES ('ARC_STUDENTS','Archive Student batch process','A year-end archive process that will set student status of CUR (current) to ARC (archived)',100,TIMESTAMP'2023-09-01  00:00:00.0');

INSERT INTO BATCH_JOB_TYPE_CODE (BATCH_JOB_TYPE_CODE,LABEL,DESCRIPTION,DISPLAY_ORDER,EFFECTIVE_DATE)
VALUES ('ARC_SCH_REPORTS','Archive School Reports process','A year-end archive process that sets the graduated, not-yet graduated and projected to graduated school reports to archive status',110,TIMESTAMP'2023-09-01  00:00:00.0');
