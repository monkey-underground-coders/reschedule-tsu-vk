package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageResponse;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class EmptyTextQueryProcessor implements TextQueryProcessor {
	@Override
	public CompletionStage<MessageResponse> process(UserInfo userInfo, ExtendedMessage extendedMessage) {
		return CompletableFuture.completedFuture(null);
	}
}
