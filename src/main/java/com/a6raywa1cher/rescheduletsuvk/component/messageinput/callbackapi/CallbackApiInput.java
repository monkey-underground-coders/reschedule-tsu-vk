package com.a6raywa1cher.rescheduletsuvk.component.messageinput.callbackapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
