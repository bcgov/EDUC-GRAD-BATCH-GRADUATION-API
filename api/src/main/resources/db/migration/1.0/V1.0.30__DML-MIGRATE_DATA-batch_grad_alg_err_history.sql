INSERT INTO BATCH_GRAD_ALG_STUDENT(graduation_student_record_id, job_execution_id, error, create_user, create_date, update_user, update_date)
SELECT graduation_student_record_id, job_execution_id, error, create_user, create_date, update_user, update_date FROM BATCH_GRAD_ALG_ERR_HISTORY;
