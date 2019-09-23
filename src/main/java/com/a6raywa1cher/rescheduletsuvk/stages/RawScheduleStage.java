package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.dao.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.VkKeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.RawScheduleStage.NAME;

@Component(NAME)
public class RawScheduleStage implements Stage {
	public static final String NAME = "rawScheduleStage";
	private static final Logger log = LoggerFactory.getLogger(RawScheduleStage.class);
	private VkApiClient vk;
	private GroupActor group;
	private StageRouterComponent stageRouterComponent;
	private RtsServerRestComponent restComponent;
	private UserInfoService service;

	@Autowired
	public RawScheduleStage(VkApiClient vk, GroupActor group, StageRouterComponent stageRouterComponent,
	                        RtsServerRestComponent restComponent, UserInfoService service) {
		this.vk = vk;
		this.group = group;
		this.stageRouterComponent = stageRouterComponent;
		this.restComponent = restComponent;
		this.service = service;
	}

	private String toPayload(WeekSign weekSign) {
		return new ObjectMapper().createObjectNode()
				.put(ROUTE, NAME)
				.put("sign", weekSign.name())
				.toString();
	}

	private void step1(UserInfo userInfo, ExtendedMessage message) {
		restComponent.getWeekSign(userInfo.getFacultyId())
				.thenAccept(response -> {
					if (response.getWeekSign().equals(WeekSign.ANY)) {
						try {
							step2(userInfo, message);
						} catch (JsonProcessingException e) {
							stageRouterComponent.routeMessage(message, MainMenuStage.NAME);
						}
					}
					WeekSign weekSign = response.getWeekSign();
					WeekSign inverse = WeekSign.inverse(weekSign);
					VkUtils.sendMessage(vk, group, message.getUserId(),
							"Окей, выбери неделю",
							VkUtils.createKeyboard(true,
									new VkKeyboardButton(
											weekSign == WeekSign.MINUS ? VkKeyboardButton.Color.NEGATIVE :
													VkKeyboardButton.Color.POSITIVE,
											"Текущая: " + weekSign.getPrettyString(),
											toPayload(weekSign)
									),
									new VkKeyboardButton(inverse == WeekSign.MINUS ?
											VkKeyboardButton.Color.NEGATIVE :
											VkKeyboardButton.Color.POSITIVE,
											"Следующая: " + inverse.getPrettyString(),
											toPayload(inverse))));
				})
				.exceptionally(e -> {
					log.error("Get week for raw schedule sign error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step2(UserInfo userInfo, ExtendedMessage message) throws JsonProcessingException {
		JsonNode jsonNode = new ObjectMapper().readTree(message.getPayload());
		WeekSign weekSign = WeekSign.valueOf(jsonNode.get("sign").asText());
		restComponent.getRawSchedule(userInfo.getFacultyId(), userInfo.getGroupId())
				.thenAccept(response -> {
					List<LessonCellMirror> list = response.stream()
							.filter(cell -> cell.getWeekSign().equals(WeekSign.ANY) ||
									cell.getWeekSign().equals(weekSign))
							.filter(cell -> cell.getSubgroup() == 0 ||
									cell.getSubgroup().equals(userInfo.getSubgroup()))
							.collect(Collectors.toList());
					Map<DayOfWeek, List<LessonCellMirror>> sorted = new HashMap<>();
					Arrays.stream(DayOfWeek.values())
							.forEach(dayOfWeek -> sorted.put(dayOfWeek, new ArrayList<>()));
					list.forEach(mirror -> {
						sorted.get(mirror.getDayOfWeek()).add(mirror);
					});
					StringBuilder sb = new StringBuilder();
					for (int i = 1; i < 7; i++) {
						DayOfWeek dayOfWeek = DayOfWeek.of(i);
						sb.append(CommonUtils.convertLessonCells(dayOfWeek, weekSign, false,
								sorted.get(dayOfWeek), false));
					}
					VkUtils.sendMessage(vk, group, message.getUserId(), sb.toString(),
							MainMenuStage.getDefaultKeyboard());
				})
				.exceptionally(e -> {
					log.error("Get raw schedule error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	@Override
	public void accept(ExtendedMessage message) {
		Optional<UserInfo> optionalUserInfo = service.getById(message.getUserId());
		if (optionalUserInfo.isEmpty()) {
			stageRouterComponent.routeMessage(message, WelcomeStage.NAME);
			return;
		}
		if (message.getPayload() == null) {
			stageRouterComponent.routeMessage(message, MainMenuStage.NAME);
		}
		try {
			JsonNode jsonNode = new ObjectMapper().readTree(message.getPayload());
			if (!jsonNode.has("sign")) {
				step1(optionalUserInfo.get(), message);
			} else {
				step2(optionalUserInfo.get(), message);
			}
		} catch (JsonProcessingException e) {
			stageRouterComponent.routeMessage(message, MainMenuStage.NAME);
		}
	}
}
