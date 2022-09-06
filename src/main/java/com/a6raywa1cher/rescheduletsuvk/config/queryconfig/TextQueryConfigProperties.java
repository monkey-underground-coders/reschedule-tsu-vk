package com.a6raywa1cher.rescheduletsuvk.config.queryconfig;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app.text-query")
@Data
@Validated
public class TextQueryConfigProperties {
	private boolean activateDialogFlow = false;

	private String credentialsPath = "classpath:googlecredentials.json";
}
