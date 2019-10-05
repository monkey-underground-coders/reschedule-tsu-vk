package com.a6raywa1cher.rescheduletsuvk.component;

import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.MainMenuStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.MainMenuStage.NAME;

@Component
public class DefaultKeyboardsComponent {

	private MessageOutput messageOutput;
	private MainMenuStageStringsConfigProperties mainMenuStageProperties;

	@Autowired
	public DefaultKeyboardsComponent(MessageOutput messageOutput, MainMenuStageStringsConfigProperties mainMenuStageProperties) {
		this.messageOutput = messageOutput;
		this.mainMenuStageProperties = mainMenuStageProperties;
	}

	public String mainMenuStage() {
		ObjectMapper objectMapper = new ObjectMapper();
		String basicPayload = objectMapper.createObjectNode()
				.put(ROUTE, NAME)
				.toString();
		return messageOutput.createKeyboard(false, new int[]{1, 1, 1, 1, 2, 2},
				new KeyboardButton(KeyboardButton.Color.PRIMARY, mainMenuStageProperties.getGetSevenDays(), basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, mainMenuStageProperties.getGetTodayLessons(), basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, mainMenuStageProperties.getGetNextLesson(), basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, mainMenuStageProperties.getGetTomorrowLessons(), basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, mainMenuStageProperties.getGetTeacherLessons(), basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, mainMenuStageProperties.getGetRawSchedule(), basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, mainMenuStageProperties.getDropSettings(), basicPayload),
				new KeyboardButton(KeyboardButton.Color.SECONDARY, mainMenuStageProperties.getGetInfo(), basicPayload)
		);
	}
}
