package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.lessoncellrenderer.LessonCellRenderer;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.ConfigureStudentUserStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.StudentUser;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.FacultyService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
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
import org.springframework.beans.factory.annotation.Value;
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
import static com.a6raywa1cher.rescheduletsuvk.stages.ConfigureStudentUserStage.NAME;

@Component(NAME)
public class ConfigureStudentUserStage implements Stage {
	public static final String NAME = "configureStudentUserStage";
	private static final Logger log = LoggerFactory.getLogger(ConfigureStudentUserStage.class);
	@Value("${app.strings.faculty-regexp}")
	public String facultyRegex;
	@Value("${app.strings.group-regexp}")
	public String groupRegex;
	@Value("${app.strings.course-regexp}")
	public String courseRegex;
	private MessageOutput messageOutput;
	private LoadingCache<Integer, Pair<StudentUser, Integer>> userInfoLoadingCache;
	private UserInfoService service;
	private MessageRouter messageRouter;
	private AppConfigProperties properties;
	private ConfigureStudentUserStageStringsConfigProperties stringProperties;
	private ScheduleService scheduleService;
	private FacultyService facultyService;
	private LessonCellRenderer lessonCellRenderer;

	@Autowired
	public ConfigureStudentUserStage(MessageOutput messageOutput, UserInfoService service,
	                                 MessageRouter messageRouter, AppConfigProperties properties,
	                                 ConfigureStudentUserStageStringsConfigProperties stringProperties, ScheduleService scheduleService,
	                                 FacultyService facultyService, LessonCellRenderer lessonCellRenderer) {
		this.messageOutput = messageOutput;
		this.service = service;
		this.messageRouter = messageRouter;
		this.properties = properties;
		this.stringProperties = stringProperties;
		this.scheduleService = scheduleService;
		this.facultyService = facultyService;
		this.lessonCellRenderer = lessonCellRenderer;
		this.userInfoLoadingCache = CacheBuilder.newBuilder()
				.expireAfterAccess(1, TimeUnit.HOURS)
				.build(new CacheLoader<>() {
					@Override
					public Pair<StudentUser, Integer> load(Integer integer) {
						StudentUser studentUser = new StudentUser();
						studentUser.setPeerId(integer);
						return Pair.of(studentUser, 1);
					}
				});
	}

	private void step1(ExtendedMessage message, StudentUser studentUser) {
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
							stringProperties.getChooseFaculty(),
							messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})));
					userInfoLoadingCache.put(peerId, Pair.of(studentUser, 2));
				})
				.exceptionally(e -> {
					log.error("step 1 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step2(ExtendedMessage message, StudentUser studentUser) {
		Integer peerId = message.getUserId();
		String facultyId = message.getBody();
		if (facultyId == null || !facultyId.matches(facultyRegex)) {
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
					studentUser.setFacultyId(facultyId);
					List<KeyboardButton> buttons = response.stream()
							.map(GetGroupsResponse.GroupInfo::getLevel)
							.distinct()
							.sorted()
							.map(level -> new KeyboardButton(KeyboardButton.Color.SECONDARY, level, objectMapper.createObjectNode()
									.put(ROUTE, NAME)
									.toString()))
							.collect(Collectors.toList());
					messageOutput.sendMessage(message.getUserId(),
							stringProperties.getChooseLevel(),
							messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})));
					userInfoLoadingCache.put(peerId, Pair.of(studentUser, 3));
				})
				.exceptionally(e -> {
					log.error("step 2 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step3(ExtendedMessage message, StudentUser userInfo) {
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
							stringProperties.getChooseCourse(),
							messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})));
					userInfoLoadingCache.put(peerId, Pair.of(userInfo, 4));
				})
				.exceptionally(e -> {
					log.error("step 3 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step4(ExtendedMessage message, StudentUser studentUser) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		ObjectMapper objectMapper = new ObjectMapper();
		String course = message.getBody();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String level = payload.get("level").asText();
		if (course == null || !course.matches(courseRegex) || level == null ||
				(!level.equals("Бакалавриат | Специалитет") && !level.equals("Магистратура"))) {
			returnToFirstStep(message);
			return;
		}
		facultyService.getGroupsList(studentUser.getFacultyId())
				.thenAccept(response -> {
					List<KeyboardButton> buttons = response.stream()
							.filter(gi -> gi.getLevel().equals(level))
							.filter(gi -> gi.getCourse().equals(Integer.parseInt(course)))
							.map(GetGroupsResponse.GroupInfo::getName)
							.distinct()
							.sorted()
							.map(groupName -> new KeyboardButton(KeyboardButton.Color.SECONDARY,
									groupName,
									objectMapper.createObjectNode()
											.put(ROUTE, NAME)
											.put("groupName", groupName)
											.toString()))
							.collect(Collectors.toList());
					messageOutput.sendMessage(message.getUserId(),
							stringProperties.getChooseGroup(),
							messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})));
					userInfoLoadingCache.put(peerId, Pair.of(studentUser, 5));
				})
				.exceptionally(e -> {
					log.error("step 4 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step5(ExtendedMessage message, StudentUser studentUser) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String groupId = payload.get("groupName").asText();
		String facultyId = studentUser.getFacultyId();
		if (groupId == null || !groupId.matches(groupRegex)) {
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
					studentUser.setGroupId(groupId);
					if (optionalGroupInfo.get().getSubgroups() > 0) {
						return scheduleService.findDifferenceBetweenSubgroups(facultyId, optionalGroupInfo.get().getName())
								.thenAccept(diff -> {
									LessonCellMirror mirror1 = diff.getFirst();
									LessonCellMirror mirror2 = diff.getSecond();
									messageOutput.sendMessage(message.getUserId(),
											(mirror2 != null ? stringProperties.getChooseSubgroup() :
													stringProperties.getChooseSubgroupWindow()) + "\n" +
													(mirror1.getWeekSign() != WeekSign.ANY ? String.format("%s (%s)\n",
															mirror1.getDayOfWeek().getDisplayName(TextStyle.FULL,
																	Locale.forLanguageTag("ru-RU")).toUpperCase(), mirror1.getWeekSign()) :
															mirror1.getDayOfWeek().getDisplayName(TextStyle.FULL,
																	Locale.forLanguageTag("ru-RU")).toUpperCase() + "\n"
													) +
													lessonCellRenderer.convertLessonCell(mirror1, false, true, true, false, false),
											messageOutput.createKeyboard(true,
													new KeyboardButton(KeyboardButton.Color.POSITIVE, stringProperties.getYes(),
															objectMapper.createObjectNode()
																	.put(ROUTE, NAME)
																	.put("subgroup", diff.getFirstSubgroup())
																	.toString()),
													new KeyboardButton(KeyboardButton.Color.NEGATIVE, stringProperties.getNo(),
															objectMapper.createObjectNode()
																	.put(ROUTE, NAME)
																	.put("subgroup", diff.getSecondSubgroup())
																	.toString()))
									);
									userInfoLoadingCache.put(peerId, Pair.of(studentUser, 6));
								});
					} else {
						registerUser(message, studentUser);
						return CompletableFuture.completedFuture(null);
					}
				})
				.exceptionally(e -> {
					log.error("step 5 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step6(ExtendedMessage message, StudentUser studentUser) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(message.getPayload());
		int subgroup = jsonNode.get("subgroup").asInt();
		messageOutput.sendMessage(message.getUserId(),
				String.format(stringProperties.getSubgroupNotify(), subgroup));
		studentUser.setSubgroup(subgroup);
		registerUser(message, studentUser);
	}

	private void registerUser(ExtendedMessage message, StudentUser studentUser) {
		service.save(studentUser);
		log.info("Registered user {} with faculty {}, group {} and subgroup {}", message.getUserId(), studentUser.getFacultyId(), studentUser.getGroupId(), studentUser.getSubgroup());
		messageOutput.sendMessage(message.getUserId(), stringProperties.getSuccess());
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
			Pair<StudentUser, Integer> pair = userInfoLoadingCache.get(peerId);
			StudentUser studentUser = pair.getFirst();
			int step = pair.getSecond();
			switch (step) {
				case 1:
					step1(message, studentUser);
					break;
				case 2:
					step2(message, studentUser);
					break;
				case 3:
					step3(message, studentUser);
					break;
				case 4:
					step4(message, studentUser);
					break;
				case 5:
					step5(message, studentUser);
					break;
				case 6:
					step6(message, studentUser);
					break;
			}
		} catch (JsonProcessingException | ExecutionException e) {
			userInfoLoadingCache.invalidate(peerId);
			StudentUser studentUser = new StudentUser();
			studentUser.setPeerId(message.getUserId());
			step1(message, studentUser);
		}
	}
}