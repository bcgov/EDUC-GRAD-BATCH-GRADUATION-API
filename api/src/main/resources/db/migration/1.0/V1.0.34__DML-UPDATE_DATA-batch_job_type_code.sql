-- updated codes
UPDATE BATCH_JOB_TYPE_CODE SET DESCRITION = 'The Batch Transcript Verification Report (TVR) Run updates student individual TVRs as PDF reports which are a summary of a students'' GRAD status including the students'' courses and assessments, program requirements met, non-grad reasons and graduation status. This run also updates the school Projected Non-Grad Summary Report.'
WHERE BATCH_JOB_TYPE_CODE = 'TVRRUN';

UPDATE BATCH_JOB_TYPE_CODE SET LABEL = 'Credentials and Transcript Distribution Run', DESCRIPTION = 'A Credentials Distribution Run that sends printed certificate and transcript packages (including distribution reports) to schools only. Includes students with new program completions where a certificate has not yet been distributed.'
WHERE BATCH_JOB_TYPE_CODE = 'DISTRUN';

-- new codes
INSERT INTO BATCH_JOB_TYPE_CODE (BATCH_JOB_TYPE_CODE,LABEL,DESCRIPTION,DISPLAY_ORDER,EFFECTIVE_DATE,EXPIRY_DATE) VALUES ('DISTRUN_YE','Year-End Credentials and Transcript Distribution Run','A Year-End Distribution Run that sends printed certificate and transcript packages (including distribution reports) to districts and schools. Includes students with new program completions where a certificate has not yet been distributed and students with updated transcripts after a previous completion.',50,TIMESTAMP'2021-09-27  00:00:00.0',NULL);
INSERT INTO BATCH_JOB_TYPE_CODE (BATCH_JOB_TYPE_CODE,LABEL,DESCRIPTION,DISPLAY_ORDER,EFFECTIVE_DATE,EXPIRY_DATE) VALUES ('DISTRUN_SUPP','Supplemental Credentials and Transcript Distribution Run','A Supplemental Year-End Distribution Run that sends printed certificate and transcript packages (including distribution reports) to schools only. Includes students with new program completions where a certificate has not yet been distributed and students with updated transcripts after a previous completion.',70,TIMESTAMP'2021-09-27  00:00:00.0',NULL);
INSERT INTO BATCH_JOB_TYPE_CODE (BATCH_JOB_TYPE_CODE,LABEL,DESCRIPTION,DISPLAY_ORDER,EFFECTIVE_DATE,EXPIRY_DATE) VALUES ('NONGRADRUN','Non-Graduate Transcript Distribution Run','A Non-Graduate Transcript Distribution Run sends transcript packages (including distribution reports) to districts and schools for any current students in Grade 12 or AD who were on a graduation program but did not graduate.',80,TIMESTAMP'2021-09-27  00:00:00.0',NULL);

-- update the old code in batch job history
UPDATE BATCH_GRAD_ALGORITHM_JOB_HISTORY SET BATCH_JOB_TYPE_CODE = 'DISTRUN_YE'
WHERE BATCH_JOB_TYPE_CODE = 'DISTRUNYEAREND';

UPDATE BATCH_PROCESSING SET BATCH_JOB_TYPE_CODE = 'DISTRUN_YE'
WHERE BATCH_JOB_TYPE_CODE = 'DISTRUNYEAREND';

-- deleted codes
DELETE FROM BATCH_JOB_TYPE_CODE WHERE BATCH_JOB_TYPE_CODE = 'DISTRUNYEAREND';
