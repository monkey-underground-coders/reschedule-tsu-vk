package com.a6raywa1cher.rescheduletsuvk.globalstages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;

public interface GlobalListeningStage {
	boolean process(ExtendedMessage extendedMessage);
}
