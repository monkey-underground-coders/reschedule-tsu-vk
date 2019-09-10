package com.a6raywa1cher.rescheduletsuvk.component;

import com.google.gson.annotations.SerializedName;
import com.vk.api.sdk.objects.messages.Message;

public class ExtendedMessage extends Message {
	@SerializedName("payload")
	private String payload;

	public String getPayload() {
		return payload;
	}

}
