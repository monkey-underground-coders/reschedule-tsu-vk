package com.a6raywa1cher.rescheduletsuvk;

import com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RescheduleTsuVkApplication implements CommandLineRunner {

	@Autowired
	StageRouterComponent stageRouterComponent;

	public static void main(String[] args) {
		SpringApplication.run(RescheduleTsuVkApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		stageRouterComponent.startListening();
	}
}
