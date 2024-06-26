UPDATE BATCH_JOB_TYPE_CODE
SET DESCRIPTION='A year-end process that will set student status of CUR (current) and TER (terminated) to ARC (archived)', UPDATE_USER='API_GRAD_BATCH' , UPDATE_DATE=TIMESTAMP'2024-06-25  00:00:00.0'
WHERE BATCH_JOB_TYPE_CODE='ARC_STUDENTS';