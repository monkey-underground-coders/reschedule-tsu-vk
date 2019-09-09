package com.a6raywa1cher.rescheduletsuvk.stages;

import com.petersamokhin.bots.sdk.objects.Message;

public interface Stage {
	boolean applicable(Message message);

	void accept(Message message);
}
