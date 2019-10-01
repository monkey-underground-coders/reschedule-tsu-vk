package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.component.textquery.TextQueryProcessor;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.MainMenuStage.NAME;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.*;

@Component(NAME)
public class MainMenuStage implements Stage {
	public static final String NAME = "mainMenuStage";
	private static final String GET_SEVEN_DAYS = "Расписание на 7 дней";
	private static final String GET_TODAY_LESSONS = "Какие сегодня пары?";
	private static final String GET_TOMORROW_LESSONS = "Какие пары завтра (или в ПН)?";
	private static final String GET_NEXT_LESSON = "Какая следующая пара?";
	private static final String GET_TEACHER_LESSONS = "Расписание преподавателя";
	private static final String GET_RAW_SCHEDULE = "Недельное расписание";
	private static final String DROP_SETTINGS = "Изменить группу";
	private static final String GET_INFO = "Информация";
	private static final Logger log = LoggerFactory.getLogger(MainMenuStage.class);
	private MessageOutput messageOutput;
	private MessageRouter messageRouter;
	private UserInfoService service;
	private ScheduleService scheduleService;
	private TextQueryProcessor textQueryProcessor;

	@Autowired
	public MainMenuStage(MessageOutput messageOutput, MessageRouter messageRouter,
	                     UserInfoService service, ScheduleService scheduleService, TextQueryProcessor textQueryProcessor) {
		this.messageOutput = messageOutput;
		this.messageRouter = messageRouter;
		this.service = service;
		this.scheduleService = scheduleService;
		this.textQueryProcessor = textQueryProcessor;
	}

	public static String getDefaultKeyboard(MessageOutput messageOutput) {
		ObjectMapper objectMapper = new ObjectMapper();
		String basicPayload = objectMapper.createObjectNode()
				.put(ROUTE, NAME)
				.toString();
		return messageOutput.createKeyboard(false, new int[]{1, 1, 1, 1, 2, 2},
				new KeyboardButton(KeyboardButton.Color.PRIMARY, GET_SEVEN_DAYS, basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, GET_TODAY_LESSONS, basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, GET_NEXT_LESSON, basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, GET_TOMORROW_LESSONS, basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, GET_TEACHER_LESSONS, basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, GET_RAW_SCHEDULE, basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, DROP_SETTINGS, basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, GET_INFO, basicPayload)
		);
	}

	private void getSevenDays(UserInfo userInfo, ExtendedMessage message) {
		scheduleService.getScheduleForSevenDays(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(),
				LocalDate.now())
				.thenAccept(schedule -> {
					messageOutput.sendMessage(message.getUserId(),
							schedule, getDefaultKeyboard(messageOutput));
				})
				.exceptionally(e -> {
					log.error("Get seven days error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void getTodayLessons(UserInfo userInfo, ExtendedMessage extendedMessage) {
		scheduleService.getScheduleFor(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(), LocalDate.now(), true)
				.thenAccept(optional -> {
					if (optional.isEmpty()) {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								"Сегодня нет пар! Отдыхай, студент",
								getDefaultKeyboard(messageOutput));
					} else {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								optional.get(),
								getDefaultKeyboard(messageOutput));
					}
				})
				.exceptionally(e -> {
					log.error("Get today lessons error\n" + extendedMessage.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void getNextLesson(UserInfo userInfo, ExtendedMessage extendedMessage) {
		scheduleService.getNextLesson(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(), LocalDateTime.now())
				.thenAccept(optional -> {
					if (optional.isEmpty()) {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								"Больше сегодня пар не ожидается",
								getDefaultKeyboard(messageOutput));
					} else {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								optional.get(),
								getDefaultKeyboard(messageOutput));
					}
				})
				.exceptionally(e -> {
					log.error("Get next lesson error\n" + extendedMessage.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void getTomorrowLessons(UserInfo userInfo, ExtendedMessage extendedMessage) {
		LocalDate now = LocalDate.now();
		boolean isDayAfterTomorrow = false;
		LocalDate localDate;
		switch (now.getDayOfWeek()) {
			case SATURDAY:
				localDate = now.plusDays(2);
				isDayAfterTomorrow = true;
				break;
			case SUNDAY:
			default:
				localDate = now.plusDays(1);
				break;
		}
		LocalDate finalLocalDate = localDate;
		boolean finalIsDayAfterTomorrow = isDayAfterTomorrow;
		scheduleService.getScheduleFor(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(), finalLocalDate, false)
				.thenAccept(optional -> {
					if (optional.isEmpty()) {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								finalIsDayAfterTomorrow ? "Послезавтра нет пар! Отдыхай, студент" :
										"Завтра нет пар! Отдыхай, студент",
								getDefaultKeyboard(messageOutput));
					} else {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								optional.get(),
								getDefaultKeyboard(messageOutput));
					}
				})
				.exceptionally(e -> {
					log.error("Get tomorrow lessons error\n" + extendedMessage.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private boolean textQuery(UserInfo userInfo, ExtendedMessage extendedMessage) {
		return textQueryProcessor.process(userInfo, extendedMessage);
	}

	private void getRawSchedule(ExtendedMessage message) {
		messageRouter.routeMessageTo(message, RawScheduleStage.NAME);
	}

	private void dropSettings(UserInfo userInfo, ExtendedMessage message) {
		service.delete(userInfo);
		messageRouter.routeMessageTo(message, WelcomeStage.NAME);
	}

	private void greeting(UserInfo userInfo, ExtendedMessage message) {
		messageOutput.sendMessage(message.getUserId(),
				"Главное меню. Тут уютно, есть печеньки. " + COOKIES_EMOJI + '\n' +
						"Настроена " + userInfo.getGroupId() + " группа" +
						(userInfo.getSubgroup() != null ? ", " + userInfo.getSubgroup() + " подгруппа" : "") + '\n' +
						"Условные обозначения: \n" +
						CROSS_PAIR_EMOJI + " - пара у нескольких групп, \n" +
						SINGLE_SUBGROUP_EMOJI + " - пара только у одной из подгрупп.", getDefaultKeyboard(messageOutput));
	}

	private void getTeacherLessons(ExtendedMessage message) {
		messageRouter.routeMessageTo(message, FindTeacherStage.NAME);
	}

	@Override
	public void accept(ExtendedMessage message) {
		Optional<UserInfo> optionalUserInfo = service.getById(message.getUserId());
		if (optionalUserInfo.isEmpty()) {
			messageRouter.routeMessageTo(message, WelcomeStage.NAME);
			return;
		}
		if (message.getPayload() != null) {
			switch (message.getBody()) {
				case GET_SEVEN_DAYS:
					getSevenDays(optionalUserInfo.get(), message);
					break;
				case GET_TODAY_LESSONS:
					getTodayLessons(optionalUserInfo.get(), message);
					break;
				case GET_TOMORROW_LESSONS:
					getTomorrowLessons(optionalUserInfo.get(), message);
					break;
				case GET_NEXT_LESSON:
					getNextLesson(optionalUserInfo.get(), message);
					break;
				case DROP_SETTINGS:
					dropSettings(optionalUserInfo.get(), message);
					break;
				case GET_TEACHER_LESSONS:
					getTeacherLessons(message);
					break;
				case GET_RAW_SCHEDULE:
					getRawSchedule(message);
					break;
				case GET_INFO:
				default:
					greeting(optionalUserInfo.get(), message);
			}
		} else if (!textQuery(optionalUserInfo.get(), message)) {
			greeting(optionalUserInfo.get(), message);
		}
	}
}
