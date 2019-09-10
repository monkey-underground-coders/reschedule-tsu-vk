package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WelcomeStage implements PrimaryStage {
	private VkApiClient vk;
	private GroupActor groupActor;
	private RtsServerRestComponent restComponent;
	private ConfigureUserStage configureUserStage;
	private StageRouterComponent stageRouterComponent;

	@Autowired
	public WelcomeStage(VkApiClient vk, GroupActor groupActor, RtsServerRestComponent restComponent,
	                    ConfigureUserStage configureUserStage, StageRouterComponent stageRouterComponent) {
		this.vk = vk;
		this.groupActor = groupActor;
		this.restComponent = restComponent;
		this.configureUserStage = configureUserStage;
		this.stageRouterComponent = stageRouterComponent;
	}


	@Override
	public void accept(ExtendedMessage message) {
		VkUtils.sendMessage(vk, groupActor, message.getUserId(),
				"Приветствуем в ВК-боте проекта reschedule-tsu!\n" +
						"Цель этого бота - дать возможность глядеть расписание ТвГУ через ВК :D\n" +
						"\u27a1\ufe0fТелега бот: @TverSU_Timings_bot\n" +
						"Данные берутся из открытой базы данных ТвГУ, и тут пока не все факультеты.\n"
		);
		configureUserStage.accept(message);
	}

}
