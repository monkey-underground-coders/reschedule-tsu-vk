package com.a6raywa1cher.rescheduletsuvk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Component
@PropertySource(value = "classpath:strings.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "app.strings")
@Data
@Validated
public class StringsConfigProperties {
	@NotBlank
	private String crossPairEmoji;
	@NotBlank
	private String singleSubgroupEmoji;
	@NotBlank
	private String teacherEmoji;
	@NotBlank
	private String pastLessonEmoji;
	@NotBlank
	private String liveLessonEmoji;
	@NotBlank
	private String futureLessonEmoji;
	@NotBlank
	private String cookiesEmoji;
	@NotBlank
	private String arrowDownEmoji;
	@NotBlank
	private String arrowRightEmoji;
	@NotBlank
	private String groupsEmoji;
	@NotBlank
	private String[] digits;

	// regex
	@NotBlank
	private String teacherNameRegexp;
	@NotBlank
	private String facultyRegexp;
	@NotBlank
	private String groupRegexp;
	@NotBlank
	private String courseRegexp;

	// WelcomeStage
	@NotBlank
	private String welcome;

	// RawScheduleStage
	@NotBlank
	private String chooseWeekSign;
	@NotBlank
	private String currentWeek;
	@NotBlank
	private String nextWeek;

	// MainMenuStage
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
}
