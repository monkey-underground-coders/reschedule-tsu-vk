package com.a6raywa1cher.rescheduletsuvk.component.peeruserinfo;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.users.UserMin;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class VkPeerUserInfoProvider implements PeerUserInfoProvider {
	private static final Logger log = LoggerFactory.getLogger(VkPeerUserInfoProvider.class);
	private VkApiClient vk;
	private GroupActor group;

	public VkPeerUserInfoProvider(VkApiClient vk, GroupActor group) {
		this.vk = vk;
		this.group = group;
	}

	@Override
	public <T> CompletionStage<Optional<String>> getSurname(T peerId) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return vk.users().get(group)
						.userIds(Collections.singletonList(Integer.toString((Integer) peerId)))
						.execute()
						.stream()
						.map(UserMin::getLastName)
						.findFirst();
			} catch (ApiException | ClientException e) {
				log.error(String.format("Get surname of %d error", peerId), e);
				Sentry.capture(e);
				return Optional.empty();
			}
		});
	}
}
