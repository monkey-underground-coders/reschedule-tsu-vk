package com.a6raywa1cher.rescheduletsuvk.component.messageoutput;

import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;

public interface MessageOutput {
	String createKeyboard(boolean oneTime, KeyboardButton... buttonsDescriptions);

	String createKeyboard(boolean oneTime, int[] grid, KeyboardButton... buttonsDescriptions);

	String getDefaultPayload();

	<T> void sendMessage(T to, String message);

	<T> void sendMessage(T to, String message, String keyboard);
}
