package com.a6raywa1cher.rescheduletsuvk.config.vkendpoint;

import com.a6raywa1cher.rescheduletsuvk.component.VkOnlineStatusComponent;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.VkMessageOutput;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(VkConfigProperties.class)
@EnableScheduling
public class VkEndpointConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {
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

//	@Bean
//	public Object apiConfig() {
//		if (properties.isUseCallbackApi()) {
//			return new VkEndpointCallbackApiConfig(properties);
//		} else {
//			return new VkEndpointLongPollApiConfig();
//		}
//	}

	@Bean
	public VkMessageOutput vkMessageOutput(VkApiClient vk, GroupActor groupActor) {
		return new VkMessageOutput(vk, groupActor);
	}

	@Bean
	public VkOnlineStatusComponent onlineStatusComponent(VkApiClient vkApiClient, GroupActor groupActor) {
		return new VkOnlineStatusComponent(vkApiClient, groupActor);
	}

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		Set<String> strings = new HashSet<>();
		Collections.addAll(strings, environment.getActiveProfiles());
		if (strings.contains("callbackapi") || strings.contains("longpollapi")) {
			return;
		} else if (properties.isUseCallbackApi()) {
			environment.addActiveProfile("callbackapi");
		} else {
			environment.addActiveProfile("longpollapi");
		}
	}
}
