package com.a6raywa1cher.rescheduletsuvk.component.peeruserinfo;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface PeerUserInfoProvider {
	<T> CompletionStage<Optional<String>> getSurname(T peerId);
}
