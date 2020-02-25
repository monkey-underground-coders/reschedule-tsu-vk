package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageResponse;
import com.a6raywa1cher.rescheduletsuvk.component.router.RTMessageMapping;
import com.a6raywa1cher.rescheduletsuvk.component.router.RTStage;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.WelcomeStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RTStage(textQuery = "/")
public class WelcomeStage {
	private UserInfoService service;
	private WelcomeStageStringsConfigProperties properties;

	@Autowired
	public WelcomeStage(UserInfoService service, WelcomeStageStringsConfigProperties properties) {
		this.service = service;
		this.properties = properties;
	}


	@RTMessageMapping("/")
	public MessageResponse accept(ExtendedMessage message) {
		if (service.getById(message.getUserId()).isPresent()) {
			return MessageResponse.builder()
					.redirectTo("/home/")
					.build();
		} else {
			return MessageResponse.builder()
					.message(properties.getWelcome())
					.redirectTo("/configure/step1")
					.build();
		}
	}

}
