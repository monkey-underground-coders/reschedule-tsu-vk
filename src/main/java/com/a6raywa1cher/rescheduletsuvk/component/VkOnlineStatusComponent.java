package com.a6raywa1cher.rescheduletsuvk.component;

import com.vk.api.sdk.client.AbstractQueryBuilder;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.Collection;

public class VkOnlineStatusComponent {
	private static final Logger log = LoggerFactory.getLogger(RtsServerRestComponent.class);
	private VkApiClient vk;
	private GroupActor group;

	@Autowired
	public VkOnlineStatusComponent(VkApiClient vk, GroupActor group) {
		this.vk = vk;
		this.group = group;
	}

	@Scheduled(initialDelay = 0, fixedDelay = 5000000)
	public void setOnline() {
		try {
			new OnlineEnablerQueryBuilder(vk, group).execute();
		} catch (ApiException e) {
			if (!e.getMessage().contains("online is already enabled")) {
				Sentry.capture(e);
			}
			log.error("Online status enabler error", e);
		} catch (ClientException e) {
			log.error("Online status enabler error", e);
			Sentry.capture(e);
		}
	}

	private static class OnlineEnablerQueryBuilder extends AbstractQueryBuilder<OnlineEnablerQueryBuilder, Integer> {

		public OnlineEnablerQueryBuilder(VkApiClient vkApiClient, GroupActor groupActor) {
			super(vkApiClient, "groups.enableOnline", Integer.class);
			accessToken(groupActor.getAccessToken());
			groupId(groupActor.getGroupId());
		}

		protected OnlineEnablerQueryBuilder groupId(int value) {
			return unsafeParam("group_id", value);
		}

		@Override
		protected OnlineEnablerQueryBuilder getThis() {
			return this;
		}

		@Override
		protected Collection<String> essentialKeys() {
			return Arrays.asList("access_token");
		}
	}
}
