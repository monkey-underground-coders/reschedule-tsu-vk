package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.DefaultKeyboardsComponent;
import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.component.textquery.TextQueryProcessor;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.MainMenuStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.StudentUser;
import com.a6raywa1cher.rescheduletsuvk.models.TeacherUser;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.a6raywa1cher.rescheduletsuvk.stages.MainMenuStage.NAME;

@Component(NAME)
public class MainMenuStage implements Stage {
	public static final String NAME = "mainMenuStage";
	private static final Logger log = LoggerFactory.getLogger(MainMenuStage.class);
	private MessageOutput messageOutput;
	private MessageRouter messageRouter;
	private UserInfoService service;
	private ScheduleService scheduleService;
	private TextQueryProcessor textQueryProcessor;
	private MainMenuStageStringsConfigProperties properties;
	private DefaultKeyboardsComponent defaultKeyboardsComponent;

	@Autowired
	public MainMenuStage(MessageOutput messageOutput, MessageRouter messageRouter,
	                     UserInfoService service, ScheduleService scheduleService,
	                     TextQueryProcessor textQueryProcessor, MainMenuStageStringsConfigProperties properties,
	                     DefaultKeyboardsComponent defaultKeyboardsComponent) {
		this.messageOutput = messageOutput;
		this.messageRouter = messageRouter;
		this.service = service;
		this.scheduleService = scheduleService;
		this.textQueryProcessor = textQueryProcessor;
		this.properties = properties;
		this.defaultKeyboardsComponent = defaultKeyboardsComponent;
	}

	private void getSevenDays(UserInfo userInfo, ExtendedMessage message) {
		scheduleService.getScheduleForSevenDays(userInfo, LocalDate.now(), false)
				.thenAccept(schedule -> {
					messageOutput.sendMessage(message.getUserId(),
							schedule, defaultKeyboardsComponent.mainMenuStage());
				})
				.exceptionally(e -> {
					log.error("Get seven days error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void getTodayLessons(UserInfo userInfo, ExtendedMessage extendedMessage) {
		scheduleService.getScheduleFor(userInfo, LocalDate.now(), true)
				.thenAccept(optional -> {
					if (optional.isEmpty()) {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								properties.getNoLessonsToday(),
								defaultKeyboardsComponent.mainMenuStage());
					} else {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								optional.get(),
								defaultKeyboardsComponent.mainMenuStage());
					}
				})
				.exceptionally(e -> {
					log.error("Get today lessons error\n" + extendedMessage.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void getNextLesson(UserInfo userInfo, ExtendedMessage extendedMessage) {
		scheduleService.getNextLesson(userInfo, LocalDateTime.now())
				.thenAccept(optional -> {
					if (optional.isEmpty()) {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								properties.getNoNextLessonsToday(),
								defaultKeyboardsComponent.mainMenuStage());
					} else {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								optional.get(),
								defaultKeyboardsComponent.mainMenuStage());
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
		scheduleService.getScheduleFor(userInfo, finalLocalDate, false)
				.thenAccept(optional -> {
					if (optional.isEmpty()) {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								finalIsDayAfterTomorrow ? properties.getNoLessonsAtMonday() :
										properties.getNoTomorrowPairs(),
								defaultKeyboardsComponent.mainMenuStage());
					} else {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								optional.get(),
								defaultKeyboardsComponent.mainMenuStage());
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
		if (userInfo instanceof StudentUser) {
			StudentUser studentUser = (StudentUser) userInfo;
			messageOutput.sendMessage(message.getUserId(),
					studentUser.getSubgroup() != null ?
							String.format(properties.getGreetingWithSubgroup(),
									studentUser.getGroupId(), studentUser.getSubgroup()) :
							String.format(properties.getGreeting(), studentUser.getGroupId()),
					defaultKeyboardsComponent.mainMenuStage());
		} else {
			TeacherUser teacherUser = (TeacherUser) userInfo;
			messageOutput.sendMessage(message.getUserId(),
					String.format(properties.getTeacherGreeting(), teacherUser.getTeacherName()),
					defaultKeyboardsComponent.mainMenuStage());
		}
	}

	private void getTeacherLessons(ExtendedMessage message) {
		messageRouter.routeMessageTo(message, FindTeacherStage.NAME);
	}

	@Override
	public void accept(ExtendedMessage message) {
		Optional<? extends UserInfo> optionalUserInfo = service.getById(message.getUserId());
		if (optionalUserInfo.isEmpty()) {
			messageRouter.routeMessageTo(message, WelcomeStage.NAME);
			return;
		}
		if (message.getPayload() != null) {
			String body = message.getBody();
			if (properties.getGetSevenDays().equals(body)) {
				getSevenDays(optionalUserInfo.get(), message);
			} else if (properties.getGetTodayLessons().equals(body)) {
				getTodayLessons(optionalUserInfo.get(), message);
			} else if (properties.getGetTomorrowLessons().equals(body)) {
				getTomorrowLessons(optionalUserInfo.get(), message);
			} else if (properties.getGetNextLesson().equals(body)) {
				getNextLesson(optionalUserInfo.get(), message);
			} else if (properties.getDropSettings().equals(body)) {
				dropSettings(optionalUserInfo.get(), message);
			} else if (properties.getGetTeacherLessons().equals(body)) {
				getTeacherLessons(message);
			} else if (properties.getGetRawSchedule().equals(body)) {
				getRawSchedule(message);
			} else {
				greeting(optionalUserInfo.get(), message);
			}
		} else if (!textQuery(optionalUserInfo.get(), message)) {
			greeting(optionalUserInfo.get(), message);
		}
	}
}
