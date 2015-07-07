package ch.sla.jdbcperflogger.spring.test.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
interface PersonRepository extends CrudRepository<Person, Long> {

}