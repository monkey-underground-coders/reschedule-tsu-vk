package com.a6raywa1cher.rescheduletsuvk.globalstages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.objects.messages.Message;
import io.sentry.Sentry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

@Service
public class EasterEggFilterStage implements FilterStage {
	private final VkApiClient vk;
	private final GroupActor group;

	@Autowired
	public EasterEggFilterStage(VkApiClient vk, GroupActor group) {
		this.vk = vk;
		this.group = group;
	}

	@Override
	public ExtendedMessage process(ExtendedMessage extendedMessage) {
		if (extendedMessage.getBody().toLowerCase().strip().equals("спасибо")) {
			VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
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
