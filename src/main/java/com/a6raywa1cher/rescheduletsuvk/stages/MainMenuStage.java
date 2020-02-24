package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.DefaultKeyboardsComponent;
import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageResponse;
import com.a6raywa1cher.rescheduletsuvk.component.router.RTMessageMapping;
import com.a6raywa1cher.rescheduletsuvk.component.router.RTStage;
import com.a6raywa1cher.rescheduletsuvk.component.textquery.TextQueryProcessor;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.MainMenuStageStringsConfigProperties;
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
import java.util.concurrent.CompletionStage;

@Component
@RTStage(textQuery = "/home/tq")
@RTMessageMapping("/home")
public class MainMenuStage {
	private static final Logger log = LoggerFactory.getLogger(MainMenuStage.class);
	private UserInfoService service;
	private ScheduleService scheduleService;
	private TextQueryProcessor textQueryProcessor;
	private MainMenuStageStringsConfigProperties properties;
	private DefaultKeyboardsComponent defaultKeyboardsComponent;

	@Autowired
	public MainMenuStage(UserInfoService service, ScheduleService scheduleService,
	                     TextQueryProcessor textQueryProcessor, MainMenuStageStringsConfigProperties properties,
	                     DefaultKeyboardsComponent defaultKeyboardsComponent) {
		this.service = service;
		this.scheduleService = scheduleService;
		this.textQueryProcessor = textQueryProcessor;
		this.properties = properties;
		this.defaultKeyboardsComponent = defaultKeyboardsComponent;
	}

	@RTMessageMapping("/seven_days")
	public CompletionStage<MessageResponse> getSevenDays(UserInfo userInfo, ExtendedMessage message) {
		return scheduleService.getScheduleForSevenDays(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(),
				LocalDate.now())
				.thenApply(schedule -> MessageResponse.builder()
						.message(schedule)
						.keyboard(defaultKeyboardsComponent.mainMenuStage())
						.build())
				.exceptionally(e -> {
					log.error("Get seven days error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return MessageResponse.builder()
							.redirectTo("/home/info")
							.build();
				});
	}

	@RTMessageMapping("/today")
	public CompletionStage<MessageResponse> getTodayLessons(UserInfo userInfo, ExtendedMessage extendedMessage) {
		return scheduleService.getScheduleFor(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(), LocalDate.now(), true)
				.thenApply(optional -> {
					String outMessage = optional.orElse(properties.getNoLessonsToday());
					return MessageResponse.builder()
							.message(outMessage)
							.keyboard(defaultKeyboardsComponent.mainMenuStage())
							.build();
				})
				.exceptionally(e -> {
					log.error("Get today lessons error\n" + extendedMessage.toString() + "\n", e);
					Sentry.capture(e);
					return MessageResponse.builder()
							.redirectTo("/home/info")
							.build();
				});
	}

	@RTMessageMapping("/next")
	public CompletionStage<MessageResponse> getNextLesson(UserInfo userInfo, ExtendedMessage extendedMessage) {
		return scheduleService.getNextLesson(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(), LocalDateTime.now())
				.thenApply(optional -> {
					String outMessage = optional.orElse(properties.getNoNextLessonsToday());
					return MessageResponse.builder()
							.message(outMessage)
							.keyboard(defaultKeyboardsComponent.mainMenuStage())
							.build();
				})
				.exceptionally(e -> {
					log.error("Get next lesson error\n" + extendedMessage.toString() + "\n", e);
					Sentry.capture(e);
					return MessageResponse.builder()
							.redirectTo("/home/info")
							.build();
				});
	}

	@RTMessageMapping("/tomorrow")
	public CompletionStage<MessageResponse> getTomorrowLessons(UserInfo userInfo, ExtendedMessage extendedMessage) {
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
		return scheduleService.getScheduleFor(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(), finalLocalDate, false)
				.thenApply(optional -> {
					String outMessage = optional.orElse(finalIsDayAfterTomorrow ? properties.getNoLessonsAtMonday() :
							properties.getNoTomorrowPairs());
					return MessageResponse.builder()
							.message(outMessage)
							.keyboard(defaultKeyboardsComponent.mainMenuStage())
							.build();
				})
				.exceptionally(e -> {
					log.error("Get tomorrow lessons error\n" + extendedMessage.toString() + "\n", e);
					Sentry.capture(e);
					return MessageResponse.builder()
							.redirectTo("/home/info")
							.build();
				});
	}

	@RTMessageMapping("/tq")
	public CompletionStage<MessageResponse> textQuery(UserInfo userInfo, ExtendedMessage extendedMessage) {
		return textQueryProcessor.process(userInfo, extendedMessage)
				.thenApplyAsync(mr -> {
					if (mr == null) {
						return MessageResponse.builder()
								.redirectTo("/home/info")
								.build();
					}
					return mr;
				});
	}

	@RTMessageMapping("/raw")
	public MessageResponse getRawSchedule() {
		return MessageResponse.builder()
				.redirectTo("/get_raw/")
				.build();
	}

	@RTMessageMapping("/drop")
	public MessageResponse dropSettings(UserInfo userInfo, ExtendedMessage message) {
		service.delete(userInfo);
		return MessageResponse.builder()
				.redirectTo("/")
				.build();
	}

	@RTMessageMapping("/")
	public MessageResponse greeting(UserInfo userInfo) {
		String outMessage = userInfo.getSubgroup() != null ?
				String.format(properties.getGreetingWithSubgroup(),
						userInfo.getGroupId(), userInfo.getSubgroup()) :
				String.format(properties.getGreeting(), userInfo.getGroupId());
		return MessageResponse.builder()
				.message(outMessage)
				.keyboard(defaultKeyboardsComponent.mainMenuStage())
				.build();
	}

	@RTMessageMapping("/teacher")
	public MessageResponse getTeacherLessons() {
		return MessageResponse.builder()
				.redirectTo("/get_teacher/")
				.build();
	}

//	@Override
//	public void accept(ExtendedMessage message) {
//		Optional<UserInfo> optionalUserInfo = service.getById(message.getUserId());
//		if (optionalUserInfo.isEmpty()) {
//			messageRouter.routeMessageTo(message, WelcomeStage.NAME);
//			return;
//		}
//		if (message.getPayload() != null) {
//			String body = message.getBody();
//			if (properties.getGetSevenDays().equals(body)) {
//				getSevenDays(optionalUserInfo.get(), message);
//			} else if (properties.getGetTodayLessons().equals(body)) {
//				getTodayLessons(optionalUserInfo.get(), message);
//			} else if (properties.getGetTomorrowLessons().equals(body)) {
//				getTomorrowLessons(optionalUserInfo.get(), message);
//			} else if (properties.getGetNextLesson().equals(body)) {
//				getNextLesson(optionalUserInfo.get(), message);
//			} else if (properties.getDropSettings().equals(body)) {
//				dropSettings(optionalUserInfo.get(), message);
//			} else if (properties.getGetTeacherLessons().equals(body)) {
//				getTeacherLessons(message);
//			} else if (properties.getGetRawSchedule().equals(body)) {
//				getRawSchedule(message);
//			} else {
//				greeting(optionalUserInfo.get(), message);
//			}
//		} else if (!textQuery(optionalUserInfo.get(), message)) {
//			greeting(optionalUserInfo.get(), message);
//		}
//	}
}
