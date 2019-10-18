package com.a6raywa1cher.rescheduletsuvk.config.vkendpoint;

import com.a6raywa1cher.rescheduletsuvk.component.VkOnlineStatusComponent;
import com.a6raywa1cher.rescheduletsuvk.component.messageinput.CallbackApiLongPollMessageInput;
import com.a6raywa1cher.rescheduletsuvk.component.messageinput.MessageInput;
import com.a6raywa1cher.rescheduletsuvk.component.messageinput.callbackapi.CallbackApiMessageInput;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.VkMessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(VkConfigProperties.class)
@EnableScheduling
public class VkEndpointConfig {
	private final VkConfigProperties properties;

	public VkEndpointConfig(VkConfigProperties properties) {
		this.properties = properties;
	}

	@Bean
	public VkApiClient vkApiClient() {
		TransportClient transportClient = HttpTransportClient.getInstance();
		return new VkApiClient(transportClient);
	}

	@Bean
	public GroupActor serviceActor(VkConfigProperties properties) {
		return new GroupActor(properties.getGroupId(), properties.getToken());
	}

	@Bean
	public MessageInput messageInput(MessageRouter messageRouter, VkApiClient vk, GroupActor groupActor) {
		if (properties.isUseCallbackApi()) {
			return new CallbackApiMessageInput(messageRouter, properties);
		} else {
			return new CallbackApiLongPollMessageInput(vk, groupActor, messageRouter, vk, groupActor);
		}
	}

	@Bean
	public VkMessageOutput vkMessageOutput(VkApiClient vk, GroupActor groupActor) {
		return new VkMessageOutput(vk, groupActor);
	}

	@Bean
	public VkEndpointRunner vkEndpointRunner(MessageInput messageInput) {
		if (messageInput instanceof CallbackApiLongPollMessageInput) {
			return new VkEndpointRunner(messageInput);
		} else {
			return null;
		}
	}

	@Bean
	public VkOnlineStatusComponent onlineStatusComponent(VkApiClient vkApiClient, GroupActor groupActor) {
		return new VkOnlineStatusComponent(vkApiClient, groupActor);
	}
}
