package com.a6raywa1cher.rescheduletsuvk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;

@SpringBootApplication(exclude = {
		WebMvcAutoConfiguration.class
})
public class RescheduleTsuVkApplication {
	public static void main(String[] args) {
		SpringApplication.run(RescheduleTsuVkApplication.class, args);
	}
}
