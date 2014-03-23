set log 0;
SET LOCK_MODE 0;
SET UNDO_LOG 0;

-- drop everything to be sure it works even if the schema changed...
--drop all objects;

create table if not exists connection_info 
    (id identity, connectionId UUID not null, connectionNumber int not null, 
    url varchar not null, creationDate timestamp not null, connectionProperties other);

create table if not exists statement_log 
    (id identity, connectionId UUID not null, logId UUID not null, tstamp timestamp not null, statementType tinyInt not null, 
    rawSql varchar not null, filledSql varchar not null, 
    executionDurationNanos bigInt, fetchDurationNanos bigInt, nbRowsIterated int, 
    threadName varchar, exception varchar, timeout int, autoCommit boolean);

create index if not exists idx_logId on statement_log(logId);
create index if not exists idx_duration on statement_log(executionDurationNanos desc);
create index if not exists idx_rawSql on statement_log(rawSql);
create index if not exists idx_tstamp on statement_log(tstamp);
create index if not exists idx_tstamp_desc on statement_log(tstamp desc);

create table if not exists batched_statement_log 
    (id identity, logId UUID not null, batched_stmt_order int not null, filledSql varchar not null);

create index if not exists idx_batched_logId on batched_statement_log(logId);

create or replace view v_statement_log
    (id, tstamp, statementType, rawSql, filledSql, exec_plus_fetch_time, execution_time, fetch_time, nbRowsIterated, threadName, timeout, autoCommit, error, connectionNumber)
  as select statement_log.id, statement_log.tstamp, statement_log.statementType, statement_log.rawSql, statement_log.filledSql,
            statement_log.executionDurationNanos+coalesce(statement_log.fetchDurationNanos,0) as exec_plus_fetch_time,
            statement_log.executionDurationNanos as execution_time, 
            statement_log.fetchDurationNanos as fetch_time,
            statement_log.nbRowsIterated,
            statement_log.threadName,
            case when statement_log.timeout = 0 then null else statement_log.timeout end as timeout,
            case when statement_log.autoCommit then 'Y' else null end as autoCommit,
            NVL2(statement_log.exception, 1, null) as exception,
            connection_info.connectionNumber
        from statement_log join connection_info on (connection_info.connectionId=statement_log.connectionId);

--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(1, sysdate, 'raw', 'filled', 456);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(2, sysdate, 'raw', 'filled2', 456098);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(3, sysdate, 'raw', 'filled2', 456098);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(4, sysdate, 'raw', 'filled2', 456098);
--delete from statement_log where id=3;
