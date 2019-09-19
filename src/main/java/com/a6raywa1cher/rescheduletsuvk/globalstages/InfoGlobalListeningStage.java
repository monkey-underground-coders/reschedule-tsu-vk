package com.a6raywa1cher.rescheduletsuvk.globalstages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
public class InfoGlobalListeningStage implements GlobalListeningStage {
	private final VkApiClient vk;
	private final GroupActor group;
	private final BuildProperties buildProperties;

	@Autowired
	public InfoGlobalListeningStage(VkApiClient vk, GroupActor group, BuildProperties buildProperties) {
		this.vk = vk;
		this.group = group;
		this.buildProperties = buildProperties;
	}

	@Override
	public boolean process(ExtendedMessage extendedMessage) {
		if (extendedMessage.getBody().equals("!Версия")) {
			VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
					"Версия: " + buildProperties.getVersion() + ", время сборки: " +
							buildProperties.getTime().toString(), ""
			);
		}
		return false;
	}
}
