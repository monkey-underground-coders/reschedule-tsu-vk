package com.a6raywa1cher.rescheduletsuvk.component;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExtendedMessage {
	private String payload;

	private String body;

	@JsonProperty("user_id")
	private Integer userId;
}
