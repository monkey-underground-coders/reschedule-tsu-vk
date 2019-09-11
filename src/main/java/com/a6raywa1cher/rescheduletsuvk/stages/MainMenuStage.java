package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetScheduleForWeekResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.dao.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.VkKeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

import static com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.MainMenuStage.NAME;

@Component(NAME)
public class MainMenuStage implements Stage {
	public static final String NAME = "mainMenuStage";
	private static final String GET_SEVEN_DAYS = "Расписание на 7 дней";
	private VkApiClient vk;
	private GroupActor group;
	private StageRouterComponent stageRouterComponent;
	private RtsServerRestComponent restComponent;
	private UserInfoService service;

	@Autowired
	public MainMenuStage(VkApiClient vk, GroupActor group, StageRouterComponent stageRouterComponent,
	                     RtsServerRestComponent restComponent, UserInfoService service) {
		this.vk = vk;
		this.group = group;
		this.stageRouterComponent = stageRouterComponent;
		this.restComponent = restComponent;
		this.service = service;
	}

	private String getDefaultKeyboard() {
		ObjectMapper objectMapper = new ObjectMapper();
		String basicPayload = objectMapper.createObjectNode()
				.put(ROUTE, NAME)
				.toString();
		return VkUtils.createKeyboard(false,
				new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, GET_SEVEN_DAYS, basicPayload)
		);
	}

	private void getSevenDays(UserInfo userInfo, ExtendedMessage message) {
		restComponent.getScheduleForWeek(userInfo.getFacultyId(), userInfo.getGroupId())
				.thenAccept(response -> {
					StringBuilder sb = new StringBuilder();
					boolean today = true;
					for (GetScheduleForWeekResponse.Schedule schedule : response.getSchedules()) {
						sb.append(String.format("%s (%s):\n",
								schedule.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru-RU")),
								schedule.getSign().getPrettyString()
						));
						for (LessonCellMirror cellMirror : schedule.getCells()) {
							sb.append(CommonUtils.convertLessonCell(cellMirror, today)).append('\n');
						}
						sb.append('\n');
						today = false;
					}
					VkUtils.sendMessage(vk, group, message.getUserId(),
							sb.toString(), getDefaultKeyboard());
				});
	}

	private void greeting(ExtendedMessage message) {
		VkUtils.sendMessage(vk, group, message.getUserId(),
				"Главное меню. Тут уютно, есть печеньки. \uD83C\uDF6A", getDefaultKeyboard());
	}

	@Override
	public void accept(ExtendedMessage message) {
		Optional<UserInfo> optionalUserInfo = service.getById(message.getUserId());
		if (optionalUserInfo.isEmpty()) {
			stageRouterComponent.routeMessage(message, WelcomeStage.NAME);
			return;
		}
		if (message.getPayload() != null) {
			switch (message.getBody()) {
				case GET_SEVEN_DAYS:
					getSevenDays(optionalUserInfo.get(), message);
					break;
				default:
					greeting(message);
			}
		} else greeting(message);
	}
}
