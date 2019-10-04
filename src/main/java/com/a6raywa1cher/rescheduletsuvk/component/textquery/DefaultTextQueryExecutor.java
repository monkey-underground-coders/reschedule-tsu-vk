package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.config.StringsConfigProperties;
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

import static com.a6raywa1cher.rescheduletsuvk.stages.MainMenuStage.getDefaultKeyboard;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.GROUPS_EMOJI;

@Component
public class DefaultTextQueryExecutor implements TextQueryExecutor {
	private static final Logger log = LoggerFactory.getLogger(DefaultTextQueryExecutor.class);
	private MessageOutput messageOutput;
	private ScheduleService scheduleService;
	private StringsConfigProperties properties;
	private FacultyService facultyService;

	@Autowired
	public DefaultTextQueryExecutor(MessageOutput messageOutput, ScheduleService scheduleService,
	                                StringsConfigProperties properties, FacultyService facultyService) {
		this.messageOutput = messageOutput;
		this.scheduleService = scheduleService;
		this.properties = properties;
		this.facultyService = facultyService;
	}

	@Override
	public void getPair(UserInfo userInfo, ExtendedMessage extendedMessage, String groupId, LocalDate date) {
		CompletionStage<GetGroupsResponse.GroupInfo> findGroup = facultyService.getGroupsStartsWith(userInfo.getFacultyId(), groupId);
		findGroup.thenCompose(gi -> scheduleService.getScheduleFor(
				userInfo.getFacultyId(), gi.getName(), null, date, LocalDate.now().isEqual(date)))
				.thenAccept(response -> {
					if (response.isEmpty()) {
						messageOutput.sendMessage(extendedMessage.getUserId(),
								"Пары не найдены", getDefaultKeyboard(messageOutput, properties));
					} else {
						String prepared = GROUPS_EMOJI + ' ' +
								findGroup.toCompletableFuture().getNow(null).getName() + ' ' +
								response.get();
						messageOutput.sendMessage(extendedMessage.getUserId(),
								prepared, getDefaultKeyboard(messageOutput, properties));
					}
				})
				.exceptionally(e -> {
					log.error("Find group error", e);
					Sentry.capture(e);
					return null;
				});

	}
}
