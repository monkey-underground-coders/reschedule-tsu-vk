package com.a6raywa1cher.rescheduletsuvk.component.messageinput;

import com.google.gson.JsonObject;

public interface MessageInput {
	boolean parse(JsonObject json);

	void run() throws Exception;
}
