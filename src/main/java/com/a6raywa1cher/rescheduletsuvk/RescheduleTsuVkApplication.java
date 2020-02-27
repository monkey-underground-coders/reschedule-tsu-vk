package com.a6raywa1cher.rescheduletsuvk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RescheduleTsuVkApplication {
	public static void main(String[] args) {
		SpringApplication.run(RescheduleTsuVkApplication.class, args);
	}
}
