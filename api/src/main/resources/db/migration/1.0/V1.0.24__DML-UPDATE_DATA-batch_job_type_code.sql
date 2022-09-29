-- 1. disable foreign keys in these two child tables
ALTER TABLE BATCH_GRAD_ALGORITHM_JOB_HISTORY
DISABLE CONSTRAINT FK_BATCH_JOB_TYPE_CODE;

ALTER TABLE BATCH_PROCESSING
DISABLE CONSTRAINT BATCH_PROCESSING_BJTC_CODE_FK;

-- 2, UPDATE child tables' code to the new value DISTRUN
UPDATE BATCH_GRAD_ALGORITHM_JOB_HISTORY
SET BATCH_JOB_TYPE_CODE = 'DISTRUN' WHERE BATCH_JOB_TYPE_CODE = 'DISTRUNMONTH';

UPDATE BATCH_PROCESSING
SET BATCH_JOB_TYPE_CODE = 'DISTRUN' WHERE BATCH_JOB_TYPE_CODE = 'DISTRUNMONTH';

-- 3. UPDATE PARENT TABLE BATCH_JOB_TYPE_CODE
ALTER TABLE BATCH_JOB_TYPE_CODE
DISABLE CONSTRAINT BATCH_JOB_TYPE_CODE_PK;

UPDATE BATCH_JOB_TYPE_CODE
SET BATCH_JOB_TYPE_CODE = 'DISTRUN'
WHERE BATCH_JOB_TYPE_CODE = 'DISTRUNMONTH';

ALTER TABLE BATCH_JOB_TYPE_CODE
ENABLE CONSTRAINT BATCH_JOB_TYPE_CODE_PK;

-- 4, UPDATE label and description in parent table
UPDATE BATCH_JOB_TYPE_CODE
SET LABEL = 'Graduation Algorithm',
DESCRIPTION = 'The Batch Graduation Algorithm Run will determine if a student has met all of their program requirements; update their GRAD record and create the appropriate Transcript and Certificate(s).'
WHERE BATCH_JOB_TYPE_CODE = 'REGALG';

UPDATE BATCH_JOB_TYPE_CODE
SET LABEL = 'Transcript Verification Report',
DESCRIPTION = 'The Batch TVR Run produces student achievement reports (TVRs) which are a summary of a students'' GRAD status including the students'' courses and assessments, program requirements met, non-grad reasons and graduation status.'
WHERE BATCH_JOB_TYPE_CODE = 'TVRRUN';

UPDATE BATCH_JOB_TYPE_CODE
SET LABEL = 'Distribution Run',
DESCRIPTION = 'The Batch Credential Distribution Run (transcript and certificate print) sends Schools transcripts and certificates for new graduates throughout the regular school year.'
WHERE BATCH_JOB_TYPE_CODE = 'DISTRUN';

UPDATE BATCH_JOB_TYPE_CODE
SET LABEL = 'User Request Distribution Run',
DESCRIPTION = 'The User Batch Distribution Run (re)distributes transcripts and/or certificates based on the User selection criteria.'
WHERE BATCH_JOB_TYPE_CODE = 'DISTRUNUSER';

UPDATE BATCH_JOB_TYPE_CODE
SET LABEL = 'Distribution Run Year-End',
DESCRIPTION = 'The Year-End Run sends Schools transcript and certificates for new graduates as well as for current students on the Grade 12 or Adult program who have not graduated.  Aug/Sep, updated transcripts are sent to schools for students if changes have been made.'
WHERE BATCH_JOB_TYPE_CODE = 'DISTRUNYEAREND';

UPDATE BATCH_JOB_TYPE_CODE
SET LABEL = 'PSI Run FTP / Paper',
DESCRIPTION = 'The batch PSI student transcript process  supports the printing of student transcript reports to PSIs via paper printed through BC Mail or through a secure FTP process which supplies data files to the PSI.'
WHERE BATCH_JOB_TYPE_CODE = 'PSIRUN';

--5. enable foreign keys again in two child tables
ALTER TABLE BATCH_GRAD_ALGORITHM_JOB_HISTORY
ENABLE CONSTRAINT FK_BATCH_JOB_TYPE_CODE;

ALTER TABLE BATCH_PROCESSING
ENABLE CONSTRAINT BATCH_PROCESSING_BJTC_CODE_FK;