package com.example.mongoReactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MongoReactiveApplication {

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication();
		springApplication.setWebApplicationType(WebApplicationType.REACTIVE);
		springApplication.run(MongoReactiveApplication.class, args);
	}

}
