package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;

public interface TextQueryProcessor {
	boolean process(UserInfo userInfo, ExtendedMessage extendedMessage); // true - hooked, false - failed
}
