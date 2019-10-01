package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.a6raywa1cher.rescheduletsuvk.stages.WelcomeStage.NAME;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.ARROW_RIGHT_EMOJI;

@Component(NAME)
public class WelcomeStage implements PrimaryStage {
	public static final String NAME = "welcomeStage";
	private MessageRouter messageRouter;
	private UserInfoService service;
	private MessageOutput messageOutput;

	@Autowired
	public WelcomeStage(MessageRouter messageRouter, UserInfoService service, MessageOutput messageOutput) {
		this.messageRouter = messageRouter;
		this.service = service;
		this.messageOutput = messageOutput;
	}


	@Override
	public void accept(ExtendedMessage message) {
		if (service.getById(message.getUserId()).isPresent()) {
			messageRouter.routeMessageTo(message, MainMenuStage.NAME);
		} else {
			messageOutput.sendMessage(message.getUserId(),
					"Приветствуем в ВК-боте проекта reschedule-tsu!\n" +
							"Цель этого бота - дать возможность глядеть расписание ТвГУ через ВК :D\n" +
							ARROW_RIGHT_EMOJI + "Телега бот: https://teleg.run/TverSU_Timings_bot\n" +
							"Данные берутся из открытой базы данных ТвГУ, и тут пока не все факультеты.\n"
			);
			messageRouter.routeMessageTo(message, ConfigureUserStage.NAME);
		}
	}

}
