package ch.sla.jdbcperflogger.spring.test;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PersonRepository extends CrudRepository<Person, Long> {

}