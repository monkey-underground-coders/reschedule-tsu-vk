package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.DefaultKeyboardsComponent;
import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.StringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.StudentUser;
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
	private MessageOutput messageOutput;
	private ScheduleService scheduleService;
	private StringsConfigProperties properties;
	private FacultyService facultyService;
	private AppConfigProperties appConfigProperties;
	private DefaultKeyboardsComponent defaultKeyboardsComponent;

	@Autowired
	public DefaultTextQueryExecutor(MessageOutput messageOutput, ScheduleService scheduleService,
	                                StringsConfigProperties properties, FacultyService facultyService,
	                                AppConfigProperties appConfigProperties, DefaultKeyboardsComponent defaultKeyboardsComponent) {
		this.messageOutput = messageOutput;
		this.scheduleService = scheduleService;
		this.properties = properties;
		this.facultyService = facultyService;
		this.appConfigProperties = appConfigProperties;
		this.defaultKeyboardsComponent = defaultKeyboardsComponent;
	}

	@Override
	public void getPair(UserInfo userInfo, ExtendedMessage extendedMessage, String groupId, LocalDate date) {
		String facultyId;
		if (userInfo instanceof StudentUser) {
			facultyId = ((StudentUser) userInfo).getFacultyId();
		} else {
			facultyId = appConfigProperties.getPrimaryFaculty();
		}
		CompletionStage<GetGroupsResponse.GroupInfo> findGroup = facultyService.getGroupsStartsWith(facultyId, groupId);
		findGroup.thenCompose(gi -> scheduleService.getScheduleFor(
				facultyId, gi.getName(), null, date, LocalDate.now().isEqual(date)))
				.thenAccept(response -> {
					if (response.isEmpty()) {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								properties.getLessonsNotFound(), defaultKeyboardsComponent.mainMenuStage());
					} else {
						String prepared = properties.getGroupsEmoji() + ' ' +
								findGroup.toCompletableFuture().getNow(null).getName() + ' ' +
								response.get();
						messageOutput.sendMessage(extendedMessage.getUserId(),
								prepared, defaultKeyboardsComponent.mainMenuStage());
					}
				})
				.exceptionally(e -> {
					log.error("Find group error", e);
					Sentry.capture(e);
					return null;
				});

	}
}
