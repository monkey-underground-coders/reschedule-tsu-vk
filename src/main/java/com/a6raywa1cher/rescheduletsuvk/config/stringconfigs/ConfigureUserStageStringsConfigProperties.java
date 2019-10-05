package com.a6raywa1cher.rescheduletsuvk.config.stringconfigs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Component
@PropertySource(value = "classpath:strings.properties", encoding = "UTF-8")
@ConfigurationProperties(prefix = "app.strings.configure-user-stage")
@Data
@Validated
public class ConfigureUserStageStringsConfigProperties {
	@NotBlank
	private String chooseFaculty;
	@NotBlank
	private String chooseLevel;
	@NotBlank
	private String chooseCourse;
	@NotBlank
	private String chooseGroup;
	@NotBlank
	private String chooseSubgroupWindow;
	@NotBlank
	private String chooseSubgroup;
	@NotBlank
	private String yes;
	@NotBlank
	private String no;
	@NotBlank
	private String subgroupNotify;
	@NotBlank
	private String success;
}
