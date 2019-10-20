package com.a6raywa1cher.rescheduletsuvk.config.vkendpoint;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;

@ConfigurationProperties(prefix = "app.vk")
@Data
@Validated
public class VkConfigProperties {
	@Min(0)
	private Integer groupId;

	private String token;

	private boolean useCallbackApi = false;

	private String secretConfirm;

	private String secretKey;
}
