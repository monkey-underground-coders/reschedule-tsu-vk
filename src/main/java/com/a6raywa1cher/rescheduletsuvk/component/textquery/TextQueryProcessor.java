package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageResponse;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;

import java.util.concurrent.CompletionStage;

public interface TextQueryProcessor {
	CompletionStage<MessageResponse> process(UserInfo userInfo, ExtendedMessage extendedMessage); // true - hooked, false - failed
}
