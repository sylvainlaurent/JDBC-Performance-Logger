set log 0;
SET LOCK_MODE 0;
SET UNDO_LOG 0;

-- drop everything to be sure it works even if the schema changed...
--drop all objects;

create table if not exists connection_info 
    (id identity, connectionId UUID not null, connectionNumber int not null, 
    url varchar not null, creationDate timestamp not null, connectionCreationDurationNanos bigInt, connectionProperties other);

create table if not exists statement_log 
    (id identity, connectionId UUID not null, logId UUID not null, tstamp timestamp not null, statementType tinyInt not null, 
    rawSql varchar not null, filledSql varchar not null, 
    executionDurationNanos bigInt, fetchDurationNanos bigInt, rsetUsageDurationNanos bigInt, nbRows int, 
    threadName varchar, exception varchar, timeout int, autoCommit boolean, transaction_Isolation int);

create index if not exists idx_logId on statement_log(logId);
create index if not exists idx_duration on statement_log(executionDurationNanos desc);
create index if not exists idx_rawSql on statement_log(rawSql);
create index if not exists idx_tstamp on statement_log(tstamp);
create index if not exists idx_tstamp_desc on statement_log(tstamp desc);

create table if not exists batched_statement_log 
    (id identity, logId UUID not null, batched_stmt_order int not null, filledSql varchar not null);

create index if not exists idx_batched_logId on batched_statement_log(logId);

create or replace view v_statement_log
    (id, tstamp, statementType, rawSql, filledSql, EXEC_PLUS_RSET_USAGE_TIME, execution_time, fetch_time, RSET_USAGE_TIME, nbRows, threadName, timeout, autoCommit, transaction_Isolation, error, connectionNumber)
  as select statement_log.id, statement_log.tstamp, statement_log.statementType, statement_log.rawSql, statement_log.filledSql,
            statement_log.executionDurationNanos+coalesce(statement_log.rsetUsageDurationNanos,0) as EXEC_PLUS_RSET_USAGE_TIME,
            statement_log.executionDurationNanos as execution_time, 
            statement_log.fetchDurationNanos as fetch_time,
            statement_log.rsetUsageDurationNanos as rset_usage_time,
            statement_log.nbRows,
            statement_log.threadName,
            case when statement_log.timeout = 0 then null else statement_log.timeout end as timeout,
            case when statement_log.autoCommit then 'Y' else null end as autoCommit,
            statement_log.transaction_Isolation,
            NVL2(statement_log.exception, 1, null) as exception,
            connection_info.connectionNumber
        from statement_log join connection_info on (connection_info.connectionId=statement_log.connectionId);

--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(1, sysdate, 'raw', 'filled', 456);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(2, sysdate, 'raw', 'filled2', 456098);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(3, sysdate, 'raw', 'filled2', 456098);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(4, sysdate, 'raw', 'filled2', 456098);
--delete from statement_log where id=3;
