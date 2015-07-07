package ch.sla.jdbcperflogger.spring.test.mybatis;

import org.apache.ibatis.annotations.Select;

public interface PersonMapper {
    @Select("select count(*) from person")
    int countPersons();
}
