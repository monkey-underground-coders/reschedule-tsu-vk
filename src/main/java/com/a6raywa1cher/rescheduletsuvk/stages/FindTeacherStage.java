package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetScheduleOfTeacherForWeekResponse;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.VkKeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.FindTeacherStage.NAME;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.ARROW_DOWN_EMOJI;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.TEACHER_EMOJI;

@Component(NAME)
public class FindTeacherStage implements Stage {
	public static final String NAME = "findTeacherStage";
	public static final String TEACHER_NAME_REGEX = "[a-zA-Zа-яА-Я _']{1,100}";
	private static final Logger log = LoggerFactory.getLogger(FindTeacherStage.class);
	private StageRouterComponent stageRouterComponent;
	private VkApiClient vk;
	private GroupActor group;
	private RtsServerRestComponent restComponent;

	@Autowired
	public FindTeacherStage(StageRouterComponent stageRouterComponent, VkApiClient vk, GroupActor group,
	                        RtsServerRestComponent restComponent) {
		this.stageRouterComponent = stageRouterComponent;
		this.vk = vk;
		this.group = group;
		this.restComponent = restComponent;
	}

	private String getKeyboard() {
		String basicPayload = new ObjectMapper().createObjectNode()
				.put(ROUTE, NAME)
				.toString();
		return VkUtils.createKeyboard(false, new VkKeyboardButton(VkKeyboardButton.Color.NEGATIVE,
				"Выйти", basicPayload));
	}

	private void step1(ExtendedMessage message) {
		VkUtils.sendMessage(vk, group, message.getUserId(),
				"Нужно найти преподавателя? Давай попробуем найти. \n" +
						"Учти, что это - выборка из расписания, ни больше, ни меньше. ¯\\_(ツ)_/¯\n" +
						"Дается расписание на 7 рабочих дней\n" +
						ARROW_DOWN_EMOJI + "Введи фамилию или всё ФИО" + ARROW_DOWN_EMOJI,
				getKeyboard());
	}

	private void step2(ExtendedMessage message) {
		if (!message.getBody().matches(TEACHER_NAME_REGEX)) {
			VkUtils.sendMessage(vk, group, message.getUserId(), "Некорректное имя преподавателя", getKeyboard());
			return;
		}
		restComponent.findTeacher(message.getBody())
				.thenAccept(response -> {
					if (response.getTeachers() == null || response.getTeachers().isEmpty()) {
						VkUtils.sendMessage(vk, group, message.getUserId(), "Преподаватель не найден", getKeyboard());
//						returnToMainMenu(message, false);
					} else if (response.getTeachers().size() == 1) {
						step3(message, response.getTeachers().get(0));
					} else if (response.getTeachers().size() > 16) {
						VkUtils.sendMessage(vk, group, message.getUserId(),
								"Слишком много преподавателей, уточни запрос", getKeyboard());
					} else {
						VkUtils.sendMessage(vk, group, message.getUserId(), "Найдено несколько преподователей," +
								"выбери нужного", VkUtils.createKeyboard(true,
								response.getTeachers().stream()
										.map(name -> new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, name))
										.collect(Collectors.toList()).toArray(new VkKeyboardButton[]{})));
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
			VkUtils.sendMessage(vk, group, message.getUserId(), "Некорректное имя преподавателя", getKeyboard());
			return;
		}
		restComponent.getTeacherWeekSchedule(teacherName)
				.thenAccept(response -> {
					StringBuilder sb = new StringBuilder(TEACHER_EMOJI + ' ' + teacherName + '\n');
					boolean today = response.getSchedules().get(0).getDayOfWeek() == LocalDate.now().getDayOfWeek();
					for (GetScheduleOfTeacherForWeekResponse.Schedule schedule : response.getSchedules()) {
						sb.append(CommonUtils.convertLessonCells(schedule.getDayOfWeek(), schedule.getSign(),
								today, schedule.getCells(), false, false, true));
						today = false;
					}
					VkUtils.sendMessage(vk, group, message.getUserId(),
							sb.toString(), MainMenuStage.getDefaultKeyboard());
					returnToMainMenu(message, true);
				})
				.exceptionally(e -> {
					log.error("Get teacher schedule error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void returnToMainMenu(ExtendedMessage message, boolean silent) {
		stageRouterComponent.unlink(message.getUserId());
		if (!silent) {
			stageRouterComponent.routeMessage(message, MainMenuStage.NAME);
		}
	}

	@Override
	public void accept(ExtendedMessage message) {
		if (message.getBody().equals("Выйти")) {
			returnToMainMenu(message, false);
		} else if (stageRouterComponent.link(message.getUserId(), this)) {
			step1(message);
		} else if (message.getPayload() == null) {
			step2(message);
		} else {
			step3(message, message.getBody());
		}
	}
}
