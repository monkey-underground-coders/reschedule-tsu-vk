package com.a6raywa1cher.rescheduletsuvk.config.stringconfigs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@PropertySource(value = "classpath:strings.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "app.strings.raw-schedule-stage")
@Data
@Validated
public class RawScheduleStageStringsConfigProperties {
	@NotBlank
	private String chooseWeekSign;
	@NotBlank
	private String currentWeek;
	@NotBlank
	private String nextWeek;
}
