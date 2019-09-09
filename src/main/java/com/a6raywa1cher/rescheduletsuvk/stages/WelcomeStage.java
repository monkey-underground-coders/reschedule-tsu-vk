package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.utils.VkKeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.petersamokhin.bots.sdk.clients.Group;
import com.petersamokhin.bots.sdk.objects.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WelcomeStage implements PrimaryStage {
	private Group group;
	private RtsServerRestComponent restComponent;

	@Autowired
	public WelcomeStage(Group group, RtsServerRestComponent restComponent) {
		this.group = group;
		this.restComponent = restComponent;
	}

	@Override
	public void accept(Message message) {
		restComponent.getFaculties()
				.thenAccept(response -> {
					List<VkKeyboardButton> buttons = new ArrayList<>();
					for (String facultyId : response.getFaculties()) {
						buttons.add(new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, facultyId));
					}
					VkUtils.sendMessage(group, message.authorId(),
							"Приветствуем в ВК-боте проекта reschedule-tsu!\n" +
									"Цель этого бота - дать возможность глядеть расписание ТвГУ через ВК :D\n" +
									"\u27a1\ufe0fТелега бот: @TverSU_Timings_bot\n" +
									"Данные берутся из открытой базы данных ТвГУ, и тут пока не все факультеты.\n"
					);
					VkUtils.sendMessage(group, message.authorId(),
							"\u2b07\ufe0fВыбери свой, если он присутствует\u2b07\ufe0f",
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
				});
	}
}
