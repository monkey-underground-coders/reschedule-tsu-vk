package com.a6raywa1cher.rescheduletsuvk.component.router;

import io.sentry.event.Breadcrumb;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Data
public class RequestInfo {
	private final String uuid = UUID.randomUUID().toString();

	private final long start = System.currentTimeMillis();

	private List<Breadcrumb> breadcrumbList = new LinkedList<>();

	private MessageResponse previousMessageResponse = null;

	public RequestInfo resolveMessageResponse(MessageResponse messageResponse) {
		if (previousMessageResponse == null) {
			previousMessageResponse = messageResponse;
		} else {
			previousMessageResponse = previousMessageResponse.resolve(messageResponse);
		}
		return this;
	}

	public RequestInfo withBreadcrumb(Breadcrumb breadcrumb) {
		breadcrumbList.add(breadcrumb);
		return this;
	}
}
