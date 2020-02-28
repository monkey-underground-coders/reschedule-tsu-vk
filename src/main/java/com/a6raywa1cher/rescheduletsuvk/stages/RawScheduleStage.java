package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageResponse;
import com.a6raywa1cher.rescheduletsuvk.component.router.RTMessageMapping;
import com.a6raywa1cher.rescheduletsuvk.component.router.RTStage;
import com.a6raywa1cher.rescheduletsuvk.component.rts.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.RawScheduleStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;

@Component
@RTStage
@RTMessageMapping("/raw")
public class RawScheduleStage {
	private static final Logger log = LoggerFactory.getLogger(RawScheduleStage.class);
	private MessageOutput messageOutput;
	private RtsServerRestComponent restComponent;
	private RawScheduleStageStringsConfigProperties properties;
	private CommonUtils commonUtils;

	@Autowired
	public RawScheduleStage(MessageOutput messageOutput,
	                        RtsServerRestComponent restComponent, RawScheduleStageStringsConfigProperties properties,
	                        CommonUtils commonUtils) {
		this.messageOutput = messageOutput;
		this.restComponent = restComponent;
		this.properties = properties;
		this.commonUtils = commonUtils;
	}

	private String toPayload(WeekSign weekSign) {
		return new ObjectMapper().createObjectNode()
				.put(ROUTE, "/raw/step2")
				.put("sign", weekSign.name())
				.toString();
	}

	@RTMessageMapping("/step1")
	public CompletionStage<MessageResponse> step1(UserInfo userInfo) {
		return restComponent.getWeekSign(userInfo.getFacultyId())
				.thenApply(response -> {
					if (response.getWeekSign().equals(WeekSign.ANY)) {
						return MessageResponse.builder()
								.redirectTo("/raw/step2")
								.build();
					}
					WeekSign weekSign = response.getWeekSign();
					WeekSign inverse = WeekSign.inverse(weekSign);
					String keyboard = messageOutput.createKeyboard(true,
							new KeyboardButton(
									weekSign == WeekSign.MINUS ? KeyboardButton.Color.NEGATIVE :
											KeyboardButton.Color.POSITIVE,
									String.format(properties.getCurrentWeek(), weekSign.getPrettyString()),
									toPayload(weekSign)
							),
							new KeyboardButton(inverse == WeekSign.MINUS ?
									KeyboardButton.Color.NEGATIVE :
									KeyboardButton.Color.POSITIVE,
									String.format(properties.getNextWeek(), inverse.getPrettyString()),
									toPayload(inverse)));
					return MessageResponse.builder()
							.keyboard(keyboard)
							.message(properties.getChooseWeekSign())
							.build();
				});
	}

	@RTMessageMapping("/step2")
	public CompletionStage<MessageResponse> step2(UserInfo userInfo, ExtendedMessage message) throws JsonProcessingException {
		JsonNode jsonNode = new ObjectMapper().readTree(message.getPayload());
		WeekSign weekSign = WeekSign.valueOf(jsonNode.get("sign").asText());
		return restComponent.getRawSchedule(userInfo.getFacultyId(), userInfo.getGroupId())
				.thenApply(response -> {
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
						sb.append(commonUtils.convertLessonCells(dayOfWeek, weekSign, false,
								sorted.get(dayOfWeek), false));
					}
					return MessageResponse.builder()
							.message(sb.toString())
							.set("silent", true)
							.redirectTo("/")
							.build();
				});
	}
}
