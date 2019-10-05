package com.a6raywa1cher.rescheduletsuvk.config.vkendpoint;

import com.a6raywa1cher.rescheduletsuvk.component.messageinput.CallbackApiLongPollMessageInput;
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
	public CallbackApiLongPollMessageInput messageInput(MessageRouter messageRouter, VkApiClient vk, GroupActor groupActor) {
		return new CallbackApiLongPollMessageInput(vk, groupActor, messageRouter, vk, groupActor);
	}

	@Bean
	public VkMessageOutput vkMessageOutput(VkApiClient vk, GroupActor groupActor) {
		return new VkMessageOutput(vk, groupActor);
	}

	//	@Bean
//	public CommandLineRunner commandLineRunner(CallbackApiLongPollMessageInput messageInput,
//	                                           @Value("${app.strings.teacher-name-regexp}") String welcome) {
//		System.out.println(welcome);
//		return args -> {
//			while (true) {
//				try {
//					messageInput.run();
//				} catch (Exception e) {
//					Sentry.capture(e);
//					log.error("Listening error", e);
//				}
//			}
//		};
//	}
	@Bean
	public VkEndpointRunner vkEndpointRunner(CallbackApiLongPollMessageInput messageInput) {
		return new VkEndpointRunner(messageInput);
	}
}
