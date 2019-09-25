package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;

import java.time.LocalDate;

public interface TextQueryExecutor {
	void getPair(UserInfo userInfo, ExtendedMessage extendedMessage, String group, LocalDate date);
}
