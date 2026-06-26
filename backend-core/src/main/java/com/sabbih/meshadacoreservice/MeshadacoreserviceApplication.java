package com.sabbih.meshadacoreservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.sabbih.meshadacoreservice", "com.sabbih.pepperjamservice"})
@EntityScan(basePackages = {"com.sabbih.meshadacoreservice", "com.sabbih.pepperjamservice"})
@EnableJpaRepositories(basePackages = {"com.sabbih.meshadacoreservice", "com.sabbih.pepperjamservice"})
public class MeshadacoreserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeshadacoreserviceApplication.class, args);
	}

}
