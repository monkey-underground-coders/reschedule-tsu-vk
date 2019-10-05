package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.WelcomeStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.a6raywa1cher.rescheduletsuvk.stages.WelcomeStage.NAME;

@Component(NAME)
public class WelcomeStage implements PrimaryStage {
	public static final String NAME = "welcomeStage";
	private MessageRouter messageRouter;
	private UserInfoService service;
	private MessageOutput messageOutput;
	private WelcomeStageStringsConfigProperties properties;

	@Autowired
	public WelcomeStage(MessageRouter messageRouter, UserInfoService service, MessageOutput messageOutput,
	                    WelcomeStageStringsConfigProperties properties) {
		this.messageRouter = messageRouter;
		this.service = service;
		this.messageOutput = messageOutput;
		this.properties = properties;
	}


	@Override
	public void accept(ExtendedMessage message) {
		if (service.getById(message.getUserId()).isPresent()) {
			messageRouter.routeMessageTo(message, MainMenuStage.NAME);
		} else {
			messageOutput.sendMessage(message.getUserId(), properties.getWelcome());
			messageRouter.routeMessageTo(message, ConfigureUserStage.NAME);
		}
	}

}
