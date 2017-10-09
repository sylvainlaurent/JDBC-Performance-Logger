package ch.sla.jdbcperflogger.spring.test.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class MyApplication implements CommandLineRunner {
    @Autowired
    private PersonRepository repository;

    @Override
    public void run(final String... args) throws Exception {
        System.err.println(repository.findAll());
    }

    public static void main(final String[] args) throws Exception {
        SpringApplication.run(MyApplication.class, args);
    }

}
