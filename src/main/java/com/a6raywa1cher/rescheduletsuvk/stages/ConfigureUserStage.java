package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.FacultyService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.ConfigureUserStage.NAME;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.ARROW_DOWN_EMOJI;

@Component(NAME)
public class ConfigureUserStage implements Stage {
	public static final String NAME = "configureUserStage";
	public static final String FACULTY_REGEX = "[а-яА-Я, \\-0-9]{3,50}";
	public static final String GROUP_REGEX = "[а-яА-Я, \\-0-9'.(М)]{1,150}";
	public static final String COURSE_REGEX = "[1-7]";
	private static final Logger log = LoggerFactory.getLogger(ConfigureUserStage.class);
	private MessageOutput messageOutput;
	private LoadingCache<Integer, Pair<UserInfo, Integer>> userInfoLoadingCache;
	private UserInfoService service;
	private MessageRouter messageRouter;
	private AppConfigProperties properties;
	private ScheduleService scheduleService;
	private FacultyService facultyService;

	@Autowired
	public ConfigureUserStage(MessageOutput messageOutput, UserInfoService service,
	                          MessageRouter messageRouter, AppConfigProperties properties,
	                          ScheduleService scheduleService, FacultyService facultyService) {
		this.messageOutput = messageOutput;
		this.service = service;
		this.messageRouter = messageRouter;
		this.properties = properties;
		this.scheduleService = scheduleService;
		this.facultyService = facultyService;
		this.userInfoLoadingCache = CacheBuilder.newBuilder()
				.expireAfterAccess(1, TimeUnit.HOURS)
				.build(new CacheLoader<>() {
					@Override
					public Pair<UserInfo, Integer> load(Integer integer) throws Exception {
						UserInfo userInfo = new UserInfo();
						userInfo.setPeerId(integer);
						return Pair.of(userInfo, 1);
					}
				});
	}

	private void step1(ExtendedMessage message, UserInfo userInfo) {
		Integer peerId = message.getUserId();
		facultyService.getFacultiesList()
				.thenAccept(response -> {
					List<KeyboardButton> buttons = new ArrayList<>();
					ObjectMapper objectMapper = new ObjectMapper();
					response.stream()
							.sorted()
							.forEach(facultyId -> {
								boolean red = properties.getRedFaculties().contains(facultyId);
								buttons.add(new KeyboardButton(
										red ? KeyboardButton.Color.NEGATIVE : KeyboardButton.Color.SECONDARY,
										facultyId,
										objectMapper.createObjectNode()
												.put("button", "1")
												.put(ROUTE, NAME)
												.toString()));
							});
					messageOutput.sendMessage(message.getUserId(),
							"Если факультет - красный, то, возможно, у него некорректное расписание\n" +
									ARROW_DOWN_EMOJI + "Выбери свой, если он присутствует" + ARROW_DOWN_EMOJI,
							messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})));
					userInfoLoadingCache.put(peerId, Pair.of(userInfo, 2));
				})
				.exceptionally(e -> {
					log.error("step 1 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step2(ExtendedMessage message, UserInfo userInfo) {
		Integer peerId = message.getUserId();
		String facultyId = message.getBody();
		if (facultyId == null || !facultyId.matches(FACULTY_REGEX)) {
			returnToFirstStep(message);
			return;
		}
		facultyService.getGroupsList(facultyId)
				.thenAccept(response -> {
					if (response.isEmpty()) {
						returnToFirstStep(message);
						return;
					}
					ObjectMapper objectMapper = new ObjectMapper();
					userInfo.setFacultyId(facultyId);
					List<KeyboardButton> buttons = response.stream()
							.map(GetGroupsResponse.GroupInfo::getLevel)
							.distinct()
							.sorted()
							.map(level -> new KeyboardButton(KeyboardButton.Color.SECONDARY, level, objectMapper.createObjectNode()
									.put(ROUTE, NAME)
									.toString()))
							.collect(Collectors.toList());
					messageOutput.sendMessage(message.getUserId(),
							ARROW_DOWN_EMOJI + "Хорошо, теперь выбери, где ты учишься" + ARROW_DOWN_EMOJI,
							messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})));
					userInfoLoadingCache.put(peerId, Pair.of(userInfo, 3));
				})
				.exceptionally(e -> {
					log.error("step 2 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step3(ExtendedMessage message, UserInfo userInfo) {
		Integer peerId = message.getUserId();
		ObjectMapper objectMapper = new ObjectMapper();
		String level = message.getBody();
		if (level == null || (!level.equals("Бакалавриат | Специалитет") && !level.equals("Магистратура"))) {
			returnToFirstStep(message);
			return;
		}
		facultyService.getGroupsList(userInfo.getFacultyId())
				.thenAccept(response -> {
					List<KeyboardButton> buttons = response.stream()
							.filter(gi -> gi.getLevel().equals(level))
							.map(GetGroupsResponse.GroupInfo::getCourse)
							.distinct()
							.sorted()
							.map(course -> new KeyboardButton(KeyboardButton.Color.SECONDARY,
									Integer.toString(course), objectMapper.createObjectNode()
									.put(ROUTE, NAME)
									.put("level", level)
									.toString()))
							.collect(Collectors.toList());
					messageOutput.sendMessage(message.getUserId(),
							ARROW_DOWN_EMOJI + "Окей, теперь курс?" + ARROW_DOWN_EMOJI,
							messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})));
					userInfoLoadingCache.put(peerId, Pair.of(userInfo, 4));
				})
				.exceptionally(e -> {
					log.error("step 3 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step4(ExtendedMessage message, UserInfo userInfo) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		ObjectMapper objectMapper = new ObjectMapper();
		String course = message.getBody();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String level = payload.get("level").asText();
		if (course == null || !course.matches(COURSE_REGEX) || level == null ||
				(!level.equals("Бакалавриат | Специалитет") && !level.equals("Магистратура"))) {
			returnToFirstStep(message);
			return;
		}
		facultyService.getGroupsList(userInfo.getFacultyId())
				.thenAccept(response -> {
					List<KeyboardButton> buttons = response.stream()
							.filter(gi -> gi.getLevel().equals(level))
							.filter(gi -> gi.getCourse().equals(Integer.parseInt(course)))
							.map(GetGroupsResponse.GroupInfo::getName)
							.distinct()
							.sorted()
							.map(groupName -> new KeyboardButton(KeyboardButton.Color.SECONDARY,
									groupName.length() >= 40 ? groupName.substring(0, 20) + "..." +
											groupName.substring(groupName.length() - 15) : groupName,
									objectMapper.createObjectNode()
											.put(ROUTE, NAME)
											.put("groupName", groupName)
											.toString()))
							.collect(Collectors.toList());
					messageOutput.sendMessage(message.getUserId(),
							ARROW_DOWN_EMOJI + "Прекрасно, последний шаг. Твоя группа?" + ARROW_DOWN_EMOJI,
							messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})));
					userInfoLoadingCache.put(peerId, Pair.of(userInfo, 5));
				})
				.exceptionally(e -> {
					log.error("step 4 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step5(ExtendedMessage message, UserInfo userInfo) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String groupId = payload.get("groupName").asText();
		String facultyId = userInfo.getFacultyId();
		if (groupId == null || !groupId.matches(GROUP_REGEX)) {
			returnToFirstStep(message);
			return;
		}
		facultyService.getGroupsList(facultyId)
				.thenCompose(response -> {
					Optional<GetGroupsResponse.GroupInfo> optionalGroupInfo = response.stream()
							.filter(gi -> gi.getName().equals(groupId)).findAny();
					if (optionalGroupInfo.isEmpty()) {
						returnToFirstStep(message);
						return CompletableFuture.completedFuture(null);
					}
					userInfo.setGroupId(groupId);
					if (optionalGroupInfo.get().getSubgroups() > 0) {
						return scheduleService.findDifferenceBetweenSubgroups(facultyId, optionalGroupInfo.get().getName())
								.thenAccept(diff -> {
									LessonCellMirror mirror1 = diff.getFirst();
									LessonCellMirror mirror2 = diff.getSecond();
									messageOutput.sendMessage(message.getUserId(),
											"Упс, не последний. Известно, что у этой группы есть подгруппы.\n" +
													"Есть ли у тебя вот эта пара?" + (mirror2 != null ? "" : "(у другой подгруппы окно)") + "\n" +
													String.format("%s (%s)",
															mirror1.getDayOfWeek().getDisplayName(TextStyle.FULL,
																	Locale.forLanguageTag("ru-RU")),
															mirror1.getWeekSign().getPrettyString()) +
													CommonUtils.convertLessonCell(mirror1, false, true),
											messageOutput.createKeyboard(true,
													new KeyboardButton(KeyboardButton.Color.POSITIVE, "Да",
															objectMapper.createObjectNode()
																	.put(ROUTE, NAME)
																	.put("subgroup", diff.getFirstSubgroup())
																	.toString()),
													new KeyboardButton(KeyboardButton.Color.NEGATIVE, "Нет",
															objectMapper.createObjectNode()
																	.put(ROUTE, NAME)
																	.put("subgroup", diff.getSecondSubgroup())
																	.toString()))
									);
									userInfoLoadingCache.put(peerId, Pair.of(userInfo, 6));
								});
					} else {
						registerUser(message, userInfo);
						return CompletableFuture.completedFuture(null);
					}
				})
				.exceptionally(e -> {
					log.error("step 5 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step6(ExtendedMessage message, UserInfo userInfo) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(message.getPayload());
		int subgroup = jsonNode.get("subgroup").asInt();
		messageOutput.sendMessage(message.getUserId(),
				"Твоя подгруппа:" + subgroup);
		userInfo.setSubgroup(subgroup);
		registerUser(message, userInfo);
	}

	private void registerUser(ExtendedMessage message, UserInfo userInfo) {
		service.save(userInfo);
		log.info("Registered user {} with faculty {}, group {} and subgroup {}", message.getUserId(), userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup());
		messageOutput.sendMessage(message.getUserId(), "Всё настроено!");
		userInfoLoadingCache.invalidate(message.getUserId());
		messageRouter.routeMessageTo(message, MainMenuStage.NAME);
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
			Pair<UserInfo, Integer> pair = userInfoLoadingCache.get(peerId);
			UserInfo userInfo = pair.getFirst();
			int step = pair.getSecond();
			switch (step) {
				case 1:
					step1(message, userInfo);
					break;
				case 2:
					step2(message, userInfo);
					break;
				case 3:
					step3(message, userInfo);
					break;
				case 4:
					step4(message, userInfo);
					break;
				case 5:
					step5(message, userInfo);
					break;
				case 6:
					step6(message, userInfo);
					break;
			}
		} catch (JsonProcessingException | ExecutionException e) {
			userInfoLoadingCache.invalidate(peerId);
			UserInfo userInfo = new UserInfo();
			userInfo.setPeerId(message.getUserId());
			step1(message, userInfo);
		}
	}
}
