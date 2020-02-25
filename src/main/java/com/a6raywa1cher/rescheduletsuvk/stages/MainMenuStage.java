package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.DefaultKeyboardsComponent;
import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.*;
import com.a6raywa1cher.rescheduletsuvk.component.textquery.TextQueryProcessor;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.MainMenuStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
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
@RTExceptionRedirect("/home/info")
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
	public CompletionStage<MessageResponse> getSevenDays(UserInfo userInfo) {
		return scheduleService.getScheduleForSevenDays(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(),
				LocalDate.now())
				.thenApply(schedule -> MessageResponse.builder()
						.message(schedule)
						.keyboard(defaultKeyboardsComponent.mainMenuStage())
						.build());
	}

	@RTMessageMapping("/today")
	public CompletionStage<MessageResponse> getTodayLessons(UserInfo userInfo) {
		return scheduleService.getScheduleFor(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(), LocalDate.now(), true)
				.thenApply(optional -> {
					String outMessage = optional.orElse(properties.getNoLessonsToday());
					return MessageResponse.builder()
							.message(outMessage)
							.keyboard(defaultKeyboardsComponent.mainMenuStage())
							.build();
				});
	}

	@RTMessageMapping("/next")
	public CompletionStage<MessageResponse> getNextLesson(UserInfo userInfo) {
		return scheduleService.getNextLesson(userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup(), LocalDateTime.now())
				.thenApply(optional -> {
					String outMessage = optional.orElse(properties.getNoNextLessonsToday());
					return MessageResponse.builder()
							.message(outMessage)
							.keyboard(defaultKeyboardsComponent.mainMenuStage())
							.build();
				});
	}

	@RTMessageMapping("/tomorrow")
	public CompletionStage<MessageResponse> getTomorrowLessons(UserInfo userInfo) {
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
				.redirectTo("/raw/step1")
				.build();
	}

	@RTMessageMapping("/drop")
	public MessageResponse dropSettings(UserInfo userInfo) {
		service.delete(userInfo);
		return MessageResponse.builder()
				.redirectTo("/")
				.build();
	}

	@RTMessageMapping("/")
	public MessageResponse greeting(UserInfo userInfo, @RTContainerEntity(nullable = true) Boolean silent) {
		if (silent != null && silent) {
			return MessageResponse.builder()
					.keyboard(defaultKeyboardsComponent.mainMenuStage())
					.set("silent", null)
					.build();
		} else {
			String outMessage = userInfo.getSubgroup() != null ?
					String.format(properties.getGreetingWithSubgroup(),
							userInfo.getGroupId(), userInfo.getSubgroup()) :
					String.format(properties.getGreeting(), userInfo.getGroupId());
			return MessageResponse.builder()
					.message(outMessage)
					.keyboard(defaultKeyboardsComponent.mainMenuStage())
					.set("silent", null)
					.build();
		}
	}

	@RTMessageMapping("/teacher")
	public MessageResponse getTeacherLessons() {
		return MessageResponse.builder()
				.redirectTo("/home/teacher/step1")
				.build();
	}
}
