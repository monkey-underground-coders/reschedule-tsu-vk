package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.DefaultKeyboardsComponent;
import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.keyboard.KeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.*;
import com.a6raywa1cher.rescheduletsuvk.component.rts.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetScheduleOfTeacherForWeekResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.FindTeacherStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;

@Component
@RTStage(textQuery = "/home/teacher/step2")
@RTMessageMapping("/home/teacher")
@RTExceptionRedirect("/home")
public class FindTeacherStage {
	private static final Logger log = LoggerFactory.getLogger(FindTeacherStage.class);
	private MessageOutput messageOutput;
	private RtsServerRestComponent restComponent;
	private CommonUtils commonUtils;

	@Value("${app.strings.teacher-name-regexp}")
	private String teacherNameRegex;
	private FindTeacherStageStringsConfigProperties properties;

	@Autowired
	public FindTeacherStage(MessageOutput messageOutput,
							FindTeacherStageStringsConfigProperties properties, RtsServerRestComponent restComponent,
							CommonUtils commonUtils, DefaultKeyboardsComponent defaultKeyboardsComponent) {
		this.messageOutput = messageOutput;
		this.properties = properties;
		this.restComponent = restComponent;
		this.commonUtils = commonUtils;
	}

	private String getKeyboard() {
		String basicPayload = new ObjectMapper().createObjectNode()
			.put(ROUTE, "/home")
			.toString();
		return messageOutput.createKeyboard(false, new KeyboardButton(KeyboardButton.Color.NEGATIVE,
			properties.getExit(), basicPayload));
	}

	@RTMessageMapping("/step1")
	public MessageResponse step1() {
		return MessageResponse.builder()
			.message(properties.getIntro())
			.keyboard(getKeyboard())
			.build();
	}

	private String getTeacherName(ExtendedMessage message) throws JsonProcessingException {
		return message.getPayload() != null ? Optional.ofNullable(new ObjectMapper().readTree(message.getPayload())
			.get("teacherName").asText(null)).orElse(message.getBody()) : message.getBody();
	}

	@RTMessageMapping("/step2")
	public CompletionStage<MessageResponse> step2(ExtendedMessage message) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String teacherName = getTeacherName(message);
		if (!teacherName.matches(teacherNameRegex)) {
			return CompletableFuture.completedStage(MessageResponse.builder()
				.message(properties.getInvalidName())
				.keyboard(getKeyboard())
				.build());
		}
		return restComponent.findTeacher(teacherName)
			.thenApply(response -> {
				if (response.getTeachers() == null || response.getTeachers().isEmpty()) {
					return MessageResponse.builder()
						.message(properties.getInvalidName())
						.keyboard(getKeyboard())
						.build();
				} else if (response.getTeachers().size() == 1) {
					return MessageResponse.builder()
						.redirectTo("/home/teacher/step3")
						.set("teacher", response.getTeachers().get(0))
						.build();
				} else if (response.getTeachers().size() > 16) {
					return MessageResponse.builder()
						.message(properties.getTooManyTeachersInResult())
						.keyboard(getKeyboard())
						.build();
				} else {
					String keyboard = messageOutput.createKeyboard(true,
						response.getTeachers().stream()
							.sorted()
							.map(name ->
								new KeyboardButton(KeyboardButton.Color.SECONDARY, name,
									objectMapper.createObjectNode()
										.put("teacherName", name)
										.put(ROUTE, "/home/teacher/step3")
										.toString()
								))
							.collect(Collectors.toList()).toArray(new KeyboardButton[]{}));
					return MessageResponse.builder()
						.message(properties.getChooseTeacher())
						.keyboard(keyboard)
						.build();
				}
			});
	}

	@RTMessageMapping("/step3")
	public CompletionStage<MessageResponse> step3(ExtendedMessage message,
												  @RTContainerEntity(nullable = true, value = "teacher") String teacherName) {
		if (teacherName == null) {
			teacherName = message.getBody();
		}
		if (!teacherName.matches(teacherNameRegex)) {
			return CompletableFuture.completedStage(MessageResponse.builder()
				.message(properties.getInvalidName())
				.keyboard(getKeyboard())
				.build());
		}
		String finalTeacherName = teacherName;
		return restComponent.getTeacherWeekSchedule(teacherName)
			.thenApply(response -> {
				Optional<LessonCellMirror> anyCell = response.getSchedules().stream()
					.flatMap(schedule -> schedule.getCells().stream()).findAny();
				if (anyCell.isEmpty()) {
					return MessageResponse.builder()
						.message(properties.getNoLessonsFound())
						.set("silent", true)
						.redirectTo("/home")
						.build();
				}
				StringBuilder sb = new StringBuilder(MessageFormat.format(properties.getResultHeader(), finalTeacherName,
					StringUtils.defaultIfBlank(anyCell.get().getTeacherTitle(), properties.getTeacherTitlePlaceholder())
				));
				sb.append('\n');
				boolean today = response.getSchedules().get(0).getDayOfWeek() == LocalDate.now().getDayOfWeek();
				for (GetScheduleOfTeacherForWeekResponse.Schedule schedule : response.getSchedules()) {
					sb.append(commonUtils.convertLessonCells(schedule.getDayOfWeek(), schedule.getSign(),
						today, schedule.getCells(), false, false, true, true));
					today = false;
				}
				return MessageResponse.builder()
					.message(sb.toString())
					.set("silent", true)
					.redirectTo("/home")
					.build();
			});
	}
}
