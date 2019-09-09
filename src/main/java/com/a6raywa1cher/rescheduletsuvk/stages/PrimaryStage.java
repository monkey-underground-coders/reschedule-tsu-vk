package com.a6raywa1cher.rescheduletsuvk.stages;

import com.petersamokhin.bots.sdk.objects.Message;

public interface PrimaryStage extends Stage {
	@Override
	default boolean applicable(Message message) {
		return true;
	}
}
