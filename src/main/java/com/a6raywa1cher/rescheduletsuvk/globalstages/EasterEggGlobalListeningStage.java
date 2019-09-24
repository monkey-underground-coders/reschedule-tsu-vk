package com.a6raywa1cher.rescheduletsuvk.globalstages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EasterEggGlobalListeningStage implements GlobalListeningStage {
	private final VkApiClient vk;
	private final GroupActor group;

	@Autowired
	public EasterEggGlobalListeningStage(VkApiClient vk, GroupActor group) {
		this.vk = vk;
		this.group = group;
	}

	@Override
	public boolean process(ExtendedMessage extendedMessage) {
		if (extendedMessage.getBody().toLowerCase().strip().equals("спасибо")) {
			VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
					"Рад стараться :)");
		}
		return false;
	}
}
