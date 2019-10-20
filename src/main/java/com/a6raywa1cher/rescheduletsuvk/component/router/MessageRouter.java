package com.a6raywa1cher.rescheduletsuvk.component.router;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.filterstages.FilterStage;
import com.a6raywa1cher.rescheduletsuvk.stages.PrimaryStage;
import com.a6raywa1cher.rescheduletsuvk.stages.Stage;

public interface MessageRouter {
	String ROUTE = "route";

	void addStage(Stage stage, String name);

	void addFilter(FilterStage filterStage);

	void setPrimaryStage(PrimaryStage primaryStage);

	void routeMessage(ExtendedMessage message);

	void routeMessageTo(ExtendedMessage message, String stageName);

	boolean link(Integer peerId, Stage stage);

	void unlink(Integer peerId);

}
