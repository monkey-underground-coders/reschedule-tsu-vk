package com.a6raywa1cher.rescheduletsuvk.config.vkendpoint;

import com.a6raywa1cher.rescheduletsuvk.component.messageinput.MessageInput;
import com.a6raywa1cher.rescheduletsuvk.component.messageinput.callbackapi.CallbackApiMessageInput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@Profile("callbackapi")
@EnableWebMvc
public class VkEndpointCallbackApiConfig {
	private final VkConfigProperties properties;

	public VkEndpointCallbackApiConfig(VkConfigProperties properties) {
		this.properties = properties;
	}

	@Bean
	public MessageInput messageInput(MessageRouter messageRouter, VkApiClient vk, GroupActor groupActor) {
		return new CallbackApiMessageInput(messageRouter, properties);
	}
}
