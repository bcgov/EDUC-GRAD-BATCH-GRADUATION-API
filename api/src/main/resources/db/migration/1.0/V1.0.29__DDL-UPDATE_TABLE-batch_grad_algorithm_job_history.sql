ALTER TABLE API_GRAD_BATCH.BATCH_GRAD_ALGORITHM_JOB_HISTORY ADD JOB_PARAMS CLOB
LOB("JOB_PARAMS") STORE AS SECUREFILE (
    TABLESPACE "API_GRAD_BLOB_DATA" ENABLE STORAGE IN ROW
    NOCACHE LOGGING  NOCOMPRESS  KEEP_DUPLICATES);
