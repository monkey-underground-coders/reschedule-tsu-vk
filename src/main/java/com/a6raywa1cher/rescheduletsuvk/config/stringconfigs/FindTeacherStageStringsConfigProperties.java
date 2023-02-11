package com.a6raywa1cher.rescheduletsuvk.config.stringconfigs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@PropertySource(value = "classpath:strings.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "app.strings.find-teacher-stage")
@Data
@Validated
public class FindTeacherStageStringsConfigProperties {
	@NotBlank
	private String intro;
	@NotBlank
	private String invalidName;
	@NotBlank
	private String tooManyTeachersInResult;
	@NotBlank
	private String chooseTeacher;
	@NotBlank
	private String exit;
	@NotBlank
	private String resultHeader;
	@NotBlank
	private String teacherTitlePlaceholder;
	@NotBlank
	private String noLessonsFound;
}
