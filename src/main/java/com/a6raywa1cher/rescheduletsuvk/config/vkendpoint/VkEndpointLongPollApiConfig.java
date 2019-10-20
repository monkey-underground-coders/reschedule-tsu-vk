package com.a6raywa1cher.rescheduletsuvk.config.vkendpoint;

import com.a6raywa1cher.rescheduletsuvk.component.messageinput.CallbackApiLongPollMessageInput;
import com.a6raywa1cher.rescheduletsuvk.component.messageinput.MessageInput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("longpollapi")
public class VkEndpointLongPollApiConfig {
	@Bean
	public MessageInput messageInput(MessageRouter messageRouter, VkApiClient vk, GroupActor groupActor) {
		return new CallbackApiLongPollMessageInput(vk, groupActor, messageRouter, vk, groupActor);
	}

	@Bean
	public VkEndpointRunner vkEndpointRunner(MessageInput messageInput) {
		return new VkEndpointRunner(messageInput);
	}
}
