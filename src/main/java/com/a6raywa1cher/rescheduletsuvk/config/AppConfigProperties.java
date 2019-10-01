package com.a6raywa1cher.rescheduletsuvk.config;

import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app")
@Data
@Validated
public class AppConfigProperties {
	@URL
	private String rtsUrl;

	private boolean activateDialogFlow = false;

	private Set<String> redFaculties = new HashSet<>();
}
