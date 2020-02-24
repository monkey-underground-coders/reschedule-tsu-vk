package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageResponse;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;

import java.time.LocalDate;
import java.util.concurrent.CompletionStage;

public interface TextQueryExecutor {
	CompletionStage<MessageResponse> getPair(UserInfo userInfo, ExtendedMessage extendedMessage, String group, LocalDate date);
}
