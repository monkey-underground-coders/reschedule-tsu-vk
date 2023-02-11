package com.a6raywa1cher.rescheduletsuvk.config.stringconfigs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@PropertySource(value = "classpath:strings.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "app.strings.main-menu-stage")
@Data
@Validated
public class MainMenuStageStringsConfigProperties {
	@NotBlank
	private String getSevenDays;
	@NotBlank
	private String getTodayLessons;
	@NotBlank
	private String getTomorrowLessons;
	@NotBlank
	private String getNextLesson;
	@NotBlank
	private String getTeacherLessons;
	@NotBlank
	private String getRawSchedule;
	@NotBlank
	private String dropSettings;
	@NotBlank
	private String getInfo;
	@NotBlank
	private String noLessonsToday;
	@NotBlank
	private String noNextLessonsToday;
	@NotBlank
	private String noLessonsAtMonday;
	@NotBlank
	private String noTomorrowPairs;
	@NotBlank
	private String greeting;
	@NotBlank
	private String greetingWithSubgroup;
}
