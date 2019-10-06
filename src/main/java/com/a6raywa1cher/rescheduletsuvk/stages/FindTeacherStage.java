package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.DefaultKeyboardsComponent;
import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.backend.BackendComponent;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.FindTeacherStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.FindTeacherStage.NAME;

@Component(NAME)
public class FindTeacherStage implements Stage {
	public static final String NAME = "findTeacherStage";
	private static final Logger log = LoggerFactory.getLogger(FindTeacherStage.class);
	private MessageRouter messageRouter;
	private MessageOutput messageOutput;
	private BackendComponent restComponent;
	private ScheduleService scheduleService;
	@Value("${app.strings.teacher-name-regexp}")
	private String teacherNameRegex;
	private DefaultKeyboardsComponent defaultKeyboardsComponent;
	private FindTeacherStageStringsConfigProperties properties;

	@Autowired
	public FindTeacherStage(MessageRouter messageRouter, MessageOutput messageOutput,
	                        FindTeacherStageStringsConfigProperties properties, BackendComponent restComponent,
	                        ScheduleService scheduleService, DefaultKeyboardsComponent defaultKeyboardsComponent) {
		this.messageRouter = messageRouter;
		this.messageOutput = messageOutput;
		this.properties = properties;
		this.restComponent = restComponent;
		this.scheduleService = scheduleService;
		this.defaultKeyboardsComponent = defaultKeyboardsComponent;
	}

	protected String getKeyboard() {
		String basicPayload = new ObjectMapper().createObjectNode()
				.put(ROUTE, NAME)
				.toString();
		return messageOutput.createKeyboard(false, new KeyboardButton(KeyboardButton.Color.NEGATIVE,
				properties.getExit(), basicPayload));
	}

	protected void step1(ExtendedMessage message) {
		messageOutput.sendMessage(message.getUserId(),
				properties.getIntro(),
				getKeyboard());
	}

	protected void step2(ExtendedMessage message) {
		if (!message.getBody().matches(teacherNameRegex)) {
			messageOutput.sendMessage(message.getUserId(), properties.getInvalidName(), getKeyboard());
			return;
		}
		restComponent.findTeacher(message.getBody())
				.thenAccept(response -> {
					if (response.getTeachers() == null || response.getTeachers().isEmpty()) {
						messageOutput.sendMessage(message.getUserId(), properties.getInvalidName(), getKeyboard());
//						returnToMainMenu(message, false);
					} else if (response.getTeachers().size() == 1) {
						step3(message, response.getTeachers().get(0));
					} else if (response.getTeachers().size() > 16) {
						messageOutput.sendMessage(message.getUserId(),
								properties.getTooManyTeachersInResult(), getKeyboard());
					} else {
						messageOutput.sendMessage(message.getUserId(), properties.getChooseTeacher(),
								messageOutput.createKeyboard(true,
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

	protected void step3(ExtendedMessage message, String teacherName) {
		if (!message.getBody().matches(teacherNameRegex)) {
			messageOutput.sendMessage(message.getUserId(), properties.getInvalidName(), getKeyboard());
			return;
		}
		scheduleService.getScheduleForSevenDays(teacherName, LocalDate.now(), true)
				.thenAccept(result -> {
					if (result == null || result.isEmpty()) {
						messageOutput.sendMessage(message.getUserId(),
								properties.getNoLessonsFound(), defaultKeyboardsComponent.mainMenuStage());
						returnToMainMenu(message, true);
						return;
					}
					messageOutput.sendMessage(message.getUserId(),
							result, defaultKeyboardsComponent.mainMenuStage());
					returnToMainMenu(message, true);
				})
				.exceptionally(e -> {
					log.error("Get teacher schedule error\n" + message.toString() + "\n", e);
					Sentry.capture(e);
					return null;
				});
	}

	protected void returnToMainMenu(ExtendedMessage message, boolean silent) {
		messageRouter.unlink(message.getUserId());
		if (!silent) {
			messageRouter.routeMessageTo(message, MainMenuStage.NAME);
		}
	}

	@Override
	public void accept(ExtendedMessage message) {
		if (message.getBody().equals(properties.getExit())) {
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
