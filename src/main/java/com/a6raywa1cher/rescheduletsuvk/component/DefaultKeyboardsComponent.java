package com.a6raywa1cher.rescheduletsuvk.component;

import com.a6raywa1cher.rescheduletsuvk.component.keyboard.KeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.MainMenuStageStringsConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.a6raywa1cher.rescheduletsuvk.component.keyboard.KeyboardButton.Color.PRIMARY;
import static com.a6raywa1cher.rescheduletsuvk.component.keyboard.KeyboardButton.Color.SECONDARY;
import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;

@Component
public class DefaultKeyboardsComponent {

	private MessageOutput messageOutput;
	private MainMenuStageStringsConfigProperties mainMenuStageProperties;

	@Autowired
	public DefaultKeyboardsComponent(MessageOutput messageOutput, MainMenuStageStringsConfigProperties mainMenuStageProperties) {
		this.messageOutput = messageOutput;
		this.mainMenuStageProperties = mainMenuStageProperties;
	}

	private String getPayload(String path) {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.createObjectNode()
			.put(ROUTE, path)
			.toString();
	}

	public String mainMenuStage() {
		return messageOutput.createKeyboard(false, new int[]{1, 1, 1, 1, 2, 2},
			new KeyboardButton(PRIMARY, mainMenuStageProperties.getGetSevenDays(), getPayload("/home/seven_days")),
			new KeyboardButton(SECONDARY, mainMenuStageProperties.getGetTodayLessons(), getPayload("/home/today")),
			new KeyboardButton(SECONDARY, mainMenuStageProperties.getGetNextLesson(), getPayload("/home/next")),
			new KeyboardButton(SECONDARY, mainMenuStageProperties.getGetTomorrowLessons(), getPayload("/home/tomorrow")),
			new KeyboardButton(SECONDARY, mainMenuStageProperties.getGetTeacherLessons(), getPayload("/home/teacher")),
			new KeyboardButton(SECONDARY, mainMenuStageProperties.getGetRawSchedule(), getPayload("/home/raw")),
			new KeyboardButton(SECONDARY, mainMenuStageProperties.getDropSettings(), getPayload("/home/drop")),
			new KeyboardButton(SECONDARY, mainMenuStageProperties.getGetInfo(), getPayload("/home/"))
		);
	}
}
