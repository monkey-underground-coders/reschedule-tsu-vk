package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.DefaultKeyboardsComponent;
import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.backend.BackendComponent;
import com.a6raywa1cher.rescheduletsuvk.component.lessoncellrenderer.LessonCellRenderer;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.peeruserinfo.PeerUserInfoProvider;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.ConfigureTeacherUserStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.FindTeacherStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.TeacherUser;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.TeacherService;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.a6raywa1cher.rescheduletsuvk.stages.ConfigureTeacherUserStage.NAME;

@Component(NAME)
public class ConfigureTeacherUserStage extends FindTeacherStage implements Stage {
	public static final String NAME = "configureTeacherUserStage";
	private static final Logger log = LoggerFactory.getLogger(ConfigureTeacherUserStage.class);
	private MessageRouter messageRouter;
	private MessageOutput messageOutput;
	private BackendComponent restComponent;
	private ScheduleService scheduleService;
	private LessonCellRenderer lessonCellRenderer;
	private TeacherService teacherService;
	private PeerUserInfoProvider peerUserInfoProvider;

	@Value("${app.strings.teacher-name-regexp}")
	private String teacherNameRegex;
	private DefaultKeyboardsComponent defaultKeyboardsComponent;
	private LoadingCache<Integer, Pair<TeacherUser, Integer>> userInfoLoadingCache;
	private ConfigureTeacherUserStageStringsConfigProperties properties;

	public ConfigureTeacherUserStage(MessageRouter messageRouter, MessageOutput messageOutput,
	                                 BackendComponent restComponent, ScheduleService scheduleService,
	                                 LessonCellRenderer lessonCellRenderer, TeacherService teacherService,
	                                 PeerUserInfoProvider peerUserInfoProvider,
	                                 DefaultKeyboardsComponent defaultKeyboardsComponent,
	                                 ConfigureTeacherUserStageStringsConfigProperties properties,
	                                 FindTeacherStageStringsConfigProperties findTeacherStageStringsConfigProperties) {
		super(messageRouter, messageOutput, findTeacherStageStringsConfigProperties, restComponent,
				scheduleService, defaultKeyboardsComponent);
		this.messageRouter = messageRouter;
		this.messageOutput = messageOutput;
		this.restComponent = restComponent;
		this.scheduleService = scheduleService;
		this.lessonCellRenderer = lessonCellRenderer;
		this.teacherService = teacherService;
		this.peerUserInfoProvider = peerUserInfoProvider;
		this.defaultKeyboardsComponent = defaultKeyboardsComponent;
		this.properties = properties;
		this.userInfoLoadingCache = CacheBuilder.newBuilder()
				.expireAfterAccess(1, TimeUnit.HOURS)
				.build(new CacheLoader<>() {
					@Override
					public Pair<TeacherUser, Integer> load(Integer integer) {
						TeacherUser teacherUser = new TeacherUser();
						teacherUser.setPeerId(integer);
						return Pair.of(teacherUser, 1);
					}
				});
	}

	private void configureStep1(ExtendedMessage message, TeacherUser teacherUser) {
		Integer peerId = message.getUserId();
		peerUserInfoProvider.getSurname(peerId)
				.thenCompose(optional -> {
					if (optional.isPresent() && optional.get().matches(teacherNameRegex)) {
						return teacherService.findTeacher("Солдатенко" + ' ');
					} else {
						return CompletableFuture.completedFuture(Collections.emptyList());
					}
				})
				.thenAccept(list -> {
					if (list.isEmpty()) {

					} else if (list.size() == 1) {
						messageOutput.sendMessage(peerId, properties.getChooseMode(),
								messageOutput.createKeyboard(true, new int[]{1, 1},
										new KeyboardButton(KeyboardButton.Color.PRIMARY,
												list.get(0),
												messageOutput.getDefaultPayload()),
										new KeyboardButton(KeyboardButton.Color.SECONDARY,
												properties.getFindTeacherButton(),
												messageOutput.getDefaultPayload())));
					} else {

					}
					userInfoLoadingCache.put(peerId, Pair.of(teacherUser, 2));
				})
				.exceptionally(e -> {
					log.error("step 1 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void configureStep2(ExtendedMessage message, TeacherUser teacherUser) {
		Integer peerId = message.getUserId();

	}

	private void returnToFirstStep(ExtendedMessage message) {
		userInfoLoadingCache.invalidate(message.getUserId());
		accept(message);
	}

	@Override
	public void accept(ExtendedMessage message) {
		Integer peerId = message.getUserId();
		if (message.getPayload() == null) {
			userInfoLoadingCache.invalidate(peerId);
		}
		try {
			Pair<TeacherUser, Integer> pair = userInfoLoadingCache.get(peerId);
			TeacherUser teacherUser = pair.getFirst();
			int step = pair.getSecond();
			switch (step) {
				case 1:
					configureStep1(message, teacherUser);
					break;
			}
		} catch (ExecutionException e) {
			userInfoLoadingCache.invalidate(peerId);
			TeacherUser teacherUser = new TeacherUser();
			teacherUser.setPeerId(message.getUserId());
			configureStep1(message, teacherUser);
		}
	}
}
