set log 0;

-- drop everything to be sure it works even if the schema changed...
drop all objects;

create table if not exists statement_log 
    (id identity, connectionId int, logId UUID not null, tstamp timestamp not null, statementType tinyInt not null, 
    rawSql varchar not null, filledSql varchar not null, 
    executionDurationNanos bigInt not null, fetchDurationNanos bigInt, nbRowsIterated int, threadName varchar,
    exception other);

create index if not exists idx_logId on statement_log(logId);
create index if not exists idx_duration on statement_log(executionDurationNanos desc);
create index if not exists idx_rawSql on statement_log(rawSql);
create index if not exists idx_tstamp on statement_log(tstamp);

create table if not exists batched_statement_log 
    (id identity, logId UUID not null, batched_stmt_order int not null, filledSql varchar not null);
create index if not exists idx_batched_logId on batched_statement_log(logId);

create or replace view v_statement_log
    (id, tstamp, statementType, rawSql, filledSql, exec_plus_fetch_time, execution_time, fetch_time, nbRowsIterated, threadName, error, connectionId)
  as select id, tstamp, statementType, rawSql, filledSql,
            executionDurationNanos+coalesce(fetchDurationNanos,0) as exec_plus_fetch_time,
            executionDurationNanos as execution_time, 
            fetchDurationNanos as fetch_time,
            nbRowsIterated,
            threadName,
            NVL2(exception, 1, 0),
            connectionId
        from statement_log;

--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(1, sysdate, 'raw', 'filled', 456);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(2, sysdate, 'raw', 'filled2', 456098);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(3, sysdate, 'raw', 'filled2', 456098);
--insert into statement_log (id, tstamp, rawSql, filledSql, durationNanos) values(4, sysdate, 'raw', 'filled2', 456098);
--delete from statement_log where id=3;
