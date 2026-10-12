package web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EntityScan("persistence.entity")
@EnableJpaRepositories("persistence.repository")
@ComponentScan({"web", "persistence"})
public class HospitalSchedulerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HospitalSchedulerApplication.class, args);
    }
}
