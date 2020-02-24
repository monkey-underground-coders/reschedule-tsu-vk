package com.a6raywa1cher.rescheduletsuvk.component.router;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.filterstages.FilterStage;

public interface MessageRouter {
	String ROUTE = "route";

	void addFilter(FilterStage filterStage);

	void addMapping(MappingMethodInfo mappingMethodInfo);

	void routeMessage(ExtendedMessage message);

	void routeMessageToPath(ExtendedMessage message, String path);
}
