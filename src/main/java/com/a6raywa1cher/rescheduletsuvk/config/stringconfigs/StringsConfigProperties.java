package com.a6raywa1cher.rescheduletsuvk.config.stringconfigs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
	private String groupsEmoji;
	@NotBlank
	private String lessonsNotFound;
	@NotNull
	@Size(min = 10, max = 10)
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
}
