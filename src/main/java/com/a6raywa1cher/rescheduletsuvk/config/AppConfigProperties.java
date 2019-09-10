package com.a6raywa1cher.rescheduletsuvk.config;

import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Component
@ConfigurationProperties(prefix = "app")
@Data
@Validated
public class AppConfigProperties {
	@NotNull
	@Min(0)
	private Integer groupId;
	@NotBlank
	private String token;
	@URL
	@NotBlank
	private String rtsUrl;
}
