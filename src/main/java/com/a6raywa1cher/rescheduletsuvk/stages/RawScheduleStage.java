package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.RawScheduleStage.NAME;

@Component(NAME)
public class RawScheduleStage implements Stage {
	public static final String NAME = "rawScheduleStage";
	private static final Logger log = LoggerFactory.getLogger(RawScheduleStage.class);
	private MessageOutput messageOutput;
	private MessageRouter messageRouter;
	private RtsServerRestComponent restComponent;
	private UserInfoService service;

	@Autowired
	public RawScheduleStage(MessageOutput messageOutput, MessageRouter messageRouter,
	                        RtsServerRestComponent restComponent, UserInfoService service) {
		this.messageOutput = messageOutput;
		this.messageRouter = messageRouter;
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
							messageRouter.routeMessageTo(message, MainMenuStage.NAME);
						}
					}
					WeekSign weekSign = response.getWeekSign();
					WeekSign inverse = WeekSign.inverse(weekSign);
					messageOutput.sendMessage(message.getUserId(),
							"Окей, выбери неделю",
							messageOutput.createKeyboard(true,
									new KeyboardButton(
											weekSign == WeekSign.MINUS ? KeyboardButton.Color.NEGATIVE :
													KeyboardButton.Color.POSITIVE,
											"Текущая: " + weekSign.getPrettyString(),
											toPayload(weekSign)
									),
									new KeyboardButton(inverse == WeekSign.MINUS ?
											KeyboardButton.Color.NEGATIVE :
											KeyboardButton.Color.POSITIVE,
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
					messageOutput.sendMessage(message.getUserId(), sb.toString(),
							MainMenuStage.getDefaultKeyboard(messageOutput));
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
			messageRouter.routeMessageTo(message, WelcomeStage.NAME);
			return;
		}
		if (message.getPayload() == null) {
			messageRouter.routeMessageTo(message, MainMenuStage.NAME);
		}
		try {
			JsonNode jsonNode = new ObjectMapper().readTree(message.getPayload());
			if (!jsonNode.has("sign")) {
				step1(optionalUserInfo.get(), message);
			} else {
				step2(optionalUserInfo.get(), message);
			}
		} catch (JsonProcessingException e) {
			messageRouter.routeMessageTo(message, MainMenuStage.NAME);
		}
	}
}
