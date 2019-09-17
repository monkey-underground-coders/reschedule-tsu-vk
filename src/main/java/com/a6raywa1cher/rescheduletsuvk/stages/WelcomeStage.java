package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent;
import com.a6raywa1cher.rescheduletsuvk.dao.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.a6raywa1cher.rescheduletsuvk.stages.WelcomeStage.NAME;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.ARROW_RIGHT_EMOJI;

@Component(NAME)
public class WelcomeStage implements PrimaryStage {
	public static final String NAME = "welcomeStage";
	private VkApiClient vk;
	private GroupActor groupActor;
	private StageRouterComponent stageRouterComponent;
	private UserInfoService service;

	@Autowired
	public WelcomeStage(VkApiClient vk, GroupActor groupActor,
	                    StageRouterComponent stageRouterComponent, UserInfoService service) {
		this.vk = vk;
		this.groupActor = groupActor;
		this.stageRouterComponent = stageRouterComponent;
		this.service = service;
	}


	@Override
	public void accept(ExtendedMessage message) {
		if (service.getById(message.getUserId()).isPresent()) {
			stageRouterComponent.routeMessage(message, MainMenuStage.NAME);
		} else {
			VkUtils.sendMessage(vk, groupActor, message.getUserId(),
					"Приветствуем в ВК-боте проекта reschedule-tsu!\n" +
							"Цель этого бота - дать возможность глядеть расписание ТвГУ через ВК :D\n" +
							ARROW_RIGHT_EMOJI + "Телега бот: https://teleg.run/TverSU_Timings_bot\n" +
							"Данные берутся из открытой базы данных ТвГУ, и тут пока не все факультеты.\n"
			);
			stageRouterComponent.routeMessage(message, ConfigureUserStage.NAME);
		}
	}

}
