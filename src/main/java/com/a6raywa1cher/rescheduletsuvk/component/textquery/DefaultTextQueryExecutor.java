package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.DefaultKeyboardsComponent;
import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.StringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.FacultyService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.CompletionStage;


@Component
public class DefaultTextQueryExecutor implements TextQueryExecutor {
	private static final Logger log = LoggerFactory.getLogger(DefaultTextQueryExecutor.class);
	private ScheduleService scheduleService;
	private StringsConfigProperties properties;
	private FacultyService facultyService;
	private DefaultKeyboardsComponent defaultKeyboardsComponent;

	@Autowired
	public DefaultTextQueryExecutor(ScheduleService scheduleService,
									StringsConfigProperties properties, FacultyService facultyService,
									DefaultKeyboardsComponent defaultKeyboardsComponent) {
		this.scheduleService = scheduleService;
		this.properties = properties;
		this.facultyService = facultyService;
		this.defaultKeyboardsComponent = defaultKeyboardsComponent;
	}

	@Override
	public CompletionStage<MessageResponse> getPair(UserInfo userInfo, ExtendedMessage extendedMessage, String groupId, LocalDate date) {
		CompletionStage<GetGroupsResponse.GroupInfo> findGroup = facultyService.getGroupsStartsWith(userInfo.getFacultyId(), groupId);
		return findGroup.thenCompose(gi -> scheduleService.getScheduleFor(
			userInfo.getFacultyId(), gi.getName(), null, date, LocalDate.now().isEqual(date)))
			.thenApply(response -> {
				if (response.isEmpty()) {
					return MessageResponse.builder()
						.message(properties.getLessonsNotFound())
						.keyboard(defaultKeyboardsComponent.mainMenuStage())
						.build();
				} else {
					String prepared = properties.getGroupsEmoji() + ' ' +
						findGroup.toCompletableFuture().getNow(null).getName() + ' ' +
						response.get();
					return MessageResponse.builder()
						.message(prepared)
						.keyboard(defaultKeyboardsComponent.mainMenuStage())
						.build();
				}
			})
			.exceptionally(e -> {
				log.error("Find group error", e);
				Sentry.capture(e);
				return null;
			});

	}
}
