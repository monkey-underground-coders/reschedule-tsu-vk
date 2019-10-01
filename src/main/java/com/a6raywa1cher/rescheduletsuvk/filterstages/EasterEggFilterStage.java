package com.a6raywa1cher.rescheduletsuvk.filterstages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.vk.api.sdk.objects.messages.Message;
import io.sentry.Sentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

@Service
public class EasterEggFilterStage implements FilterStage {
	private MessageOutput messageOutput;

	@Autowired
	public EasterEggFilterStage(MessageOutput messageOutput) {
		this.messageOutput = messageOutput;
	}

	@Override
	public ExtendedMessage process(ExtendedMessage extendedMessage) {
		if (extendedMessage.getBody().toLowerCase().strip().equals("спасибо")) {
			messageOutput.sendMessage(extendedMessage.getUserId(),
					"Рад стараться :)");
		}
		if (extendedMessage.getBody().toLowerCase().strip().equals("солдис")) {
			try {
				Field field = Message.class.getDeclaredField("body");
				field.setAccessible(true);
				field.set((Message) extendedMessage, "Солдатенко");
			} catch (Exception e) {
				Sentry.capture(e);
				e.printStackTrace();
			}
		}
		return extendedMessage;
	}
}
