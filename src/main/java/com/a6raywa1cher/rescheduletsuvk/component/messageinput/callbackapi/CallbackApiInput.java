package com.a6raywa1cher.rescheduletsuvk.component.messageinput.callbackapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CallbackApiInput {
	@NotBlank
	private String type;

	private ObjectNode object;

	@JsonProperty("group_id")
	@NotNull
	private Integer groupId;

	@NotBlank
	private String secret;
}
