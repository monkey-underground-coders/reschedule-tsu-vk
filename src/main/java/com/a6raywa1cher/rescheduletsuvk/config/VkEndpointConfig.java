package com.a6raywa1cher.rescheduletsuvk.config;

import com.petersamokhin.bots.sdk.clients.Group;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VkEndpointConfig {
	@Bean
	public Group group(AppConfigProperties properties) {
		return new Group(properties.getToken());
	}
}
