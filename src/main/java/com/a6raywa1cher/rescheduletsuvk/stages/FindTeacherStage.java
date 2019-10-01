package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetScheduleOfTeacherForWeekResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.FindTeacherStage.NAME;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.ARROW_DOWN_EMOJI;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.TEACHER_EMOJI;

@Component(NAME)
public class FindTeacherStage implements Stage {
	public static final String NAME = "findTeacherStage";
	public static final String TEACHER_NAME_REGEX = "[a-zA-Zа-яА-Я _']{1,100}";
	private static final Logger log = LoggerFactory.getLogger(FindTeacherStage.class);
	private MessageRouter messageRouter;
	private MessageOutput messageOutput;
	private RtsServerRestComponent restComponent;

	@Autowired
	public FindTeacherStage(MessageRouter messageRouter, MessageOutput messageOutput,
	                        RtsServerRestComponent restComponent) {
		this.messageRouter = messageRouter;
		this.messageOutput = messageOutput;
		this.restComponent = restComponent;
	}

	private String getKeyboard() {
		String basicPayload = new ObjectMapper().createObjectNode()
				.put(ROUTE, NAME)
				.toString();
		return messageOutput.createKeyboard(false, new KeyboardButton(KeyboardButton.Color.NEGATIVE,
				"Выйти", basicPayload));
	}

	private void step1(ExtendedMessage message) {
		messageOutput.sendMessage(message.getUserId(),
				"Нужно найти преподавателя? Давай попробуем найти. \n" +
						"Учти, что это - выборка из расписания, ни больше, ни меньше. ¯\\_(ツ)_/¯\n" +
						"Дается расписание на 7 рабочих дней\n" +
						ARROW_DOWN_EMOJI + "Введи фамилию или всё ФИО" + ARROW_DOWN_EMOJI,
				getKeyboard());
	}

	private void step2(ExtendedMessage message) {
		if (!message.getBody().matches(TEACHER_NAME_REGEX)) {
			messageOutput.sendMessage(message.getUserId(), "Некорректное имя преподавателя", getKeyboard());
			return;
		}
		restComponent.findTeacher(message.getBody())
				.thenAccept(response -> {
					if (response.getTeachers() == null || response.getTeachers().isEmpty()) {
						messageOutput.sendMessage(message.getUserId(), "Преподаватель не найден", getKeyboard());
//						returnToMainMenu(message, false);
					} else if (response.getTeachers().size() == 1) {
						step3(message, response.getTeachers().get(0));
					} else if (response.getTeachers().size() > 16) {
						messageOutput.sendMessage(message.getUserId(),
								"Слишком много преподавателей, уточни запрос", getKeyboard());
					} else {
						messageOutput.sendMessage(message.getUserId(), "Найдено несколько преподователей, " +
								"выбери нужного", messageOutput.createKeyboard(true,
								response.getTeachers().stream()
										.sorted()
										.map(name -> new KeyboardButton(KeyboardButton.Color.SECONDARY, name))
										.collect(Collectors.toList()).toArray(new KeyboardButton[]{})));
					}
				})
				.exceptionally(e -> {
					log.error("Find teacher error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step3(ExtendedMessage message, String teacherName) {
		if (!message.getBody().matches(TEACHER_NAME_REGEX)) {
			messageOutput.sendMessage(message.getUserId(), "Некорректное имя преподавателя", getKeyboard());
			return;
		}
		restComponent.getTeacherWeekSchedule(teacherName)
				.thenAccept(response -> {
					Optional<LessonCellMirror> anyCell = response.getSchedules().stream()
							.flatMap(schedule -> schedule.getCells().stream()).findFirst();
					StringBuilder sb = new StringBuilder(TEACHER_EMOJI + ' ' + teacherName);
					;
					if (anyCell.isPresent() && anyCell.get().getTeacherTitle() != null) {
						sb.append(", ").append(anyCell.get().getTeacherTitle());
					}
					sb.append('\n');
					boolean today = response.getSchedules().get(0).getDayOfWeek() == LocalDate.now().getDayOfWeek();
					for (GetScheduleOfTeacherForWeekResponse.Schedule schedule : response.getSchedules()) {
						sb.append(CommonUtils.convertLessonCells(schedule.getDayOfWeek(), schedule.getSign(),
								today, schedule.getCells(), false, false, true, true));
						today = false;
					}
					messageOutput.sendMessage(message.getUserId(),
							sb.toString(), MainMenuStage.getDefaultKeyboard(messageOutput));
					returnToMainMenu(message, true);
				})
				.exceptionally(e -> {
					log.error("Get teacher schedule error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void returnToMainMenu(ExtendedMessage message, boolean silent) {
		messageRouter.unlink(message.getUserId());
		if (!silent) {
			messageRouter.routeMessageTo(message, MainMenuStage.NAME);
		}
	}

	@Override
	public void accept(ExtendedMessage message) {
		if (message.getBody().equals("Выйти")) {
			returnToMainMenu(message, false);
		} else if (messageRouter.link(message.getUserId(), this)) {
			step1(message);
		} else if (message.getPayload() == null) {
			step2(message);
		} else {
			step3(message, message.getBody());
		}
	}
}
