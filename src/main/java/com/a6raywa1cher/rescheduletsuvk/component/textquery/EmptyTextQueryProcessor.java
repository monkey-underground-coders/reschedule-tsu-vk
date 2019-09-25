package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;

public class EmptyTextQueryProcessor implements TextQueryProcessor {
	@Override
	public boolean process(UserInfo userInfo, ExtendedMessage extendedMessage) {
		return false;
	}
}
