package com.a6raywa1cher.rescheduletsuvk.filterstages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
public class InfoFilterStage implements FilterStage {
	private MessageOutput messageOutput;
	private final BuildProperties buildProperties;

	@Autowired
	public InfoFilterStage(MessageOutput messageOutput, BuildProperties buildProperties) {
		this.messageOutput = messageOutput;
		this.buildProperties = buildProperties;
	}

	@Override
	public ExtendedMessage process(ExtendedMessage extendedMessage) {
		if (extendedMessage.getBody().equals("!Версия")) {
			messageOutput.sendMessage(extendedMessage.getUserId(),
					"Версия: " + buildProperties.getVersion() + ", время сборки: " +
							buildProperties.getTime().toString(), ""
			);
		}
		return extendedMessage;
	}
}
