package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.component.router.*;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.ConfigureUserStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.FacultyService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter.ROUTE;

@Component
@RTStage
@RTMessageMapping("/configure")
public class ConfigureUserStage {
	private static final Logger log = LoggerFactory.getLogger(ConfigureUserStage.class);
	@Value("${app.strings.faculty-regexp}")
	public String facultyRegex;
	@Value("${app.strings.group-regexp}")
	public String groupRegex;
	@Value("${app.strings.course-regexp}")
	public String courseRegex;
	private MessageOutput messageOutput;
	private UserInfoService service;
	private MessageRouter messageRouter;
	private AppConfigProperties properties;
	private ConfigureUserStageStringsConfigProperties stringProperties;
	private ScheduleService scheduleService;
	private FacultyService facultyService;
	private CommonUtils commonUtils;

	@Autowired
	public ConfigureUserStage(MessageOutput messageOutput, UserInfoService service,
	                          MessageRouter messageRouter, AppConfigProperties properties,
	                          ConfigureUserStageStringsConfigProperties stringProperties, ScheduleService scheduleService,
	                          FacultyService facultyService, CommonUtils commonUtils) {
		this.messageOutput = messageOutput;
		this.service = service;
		this.messageRouter = messageRouter;
		this.properties = properties;
		this.stringProperties = stringProperties;
		this.scheduleService = scheduleService;
		this.facultyService = facultyService;
		this.commonUtils = commonUtils;
	}

	@RTMessageMapping("/step1")
	public CompletionStage<MessageResponse> step1(ExtendedMessage message) {
		UserInfo userInfo = new UserInfo();
		userInfo.setPeerId(message.getUserId());
		return facultyService.getFacultiesList()
				.thenApply(response -> {
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
												.put("facultyId", facultyId)
												.put(ROUTE, "/configure/step2")
												.toString()));
							});
					return MessageResponse.builder()
							.message(stringProperties.getChooseFaculty())
							.keyboard(messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})))
							.set("userInfo", userInfo)
							.build();
				});
	}

	@RTMessageMapping("/step2")
	public CompletionStage<MessageResponse> step2(ExtendedMessage message, @RTContainerEntity UserInfo userInfo)
			throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(message.getPayload());
		String facultyId;
		if (!jsonNode.has("facultyId")) {
			return returnToFirstStepCF();
		} else if (!(facultyId = jsonNode.get("facultyId").asText()).matches(facultyRegex)) {
			Sentry.capture("Invalid facultyId: " + facultyId);
			return returnToFirstStepCF();
		}
		return facultyService.getGroupsList(facultyId)
				.thenApply(response -> {
					if (response.isEmpty()) {
						return returnToFirstStep();
					}
					userInfo.setFacultyId(facultyId);
					List<KeyboardButton> buttons = response.stream()
							.map(GetGroupsResponse.GroupInfo::getLevel)
							.distinct()
							.sorted()
							.map(level -> new KeyboardButton(KeyboardButton.Color.SECONDARY, level, objectMapper.createObjectNode()
									.put(ROUTE, "/configure/step3")
									.toString()))
							.collect(Collectors.toList());
					return MessageResponse.builder()
							.message(stringProperties.getChooseLevel())
							.keyboard(messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})))
							.build();
				});
	}

	private MessageResponse returnToFirstStep() {
		return MessageResponse.builder().redirectTo("/configure/step1").build();
	}

	private CompletableFuture<MessageResponse> returnToFirstStepCF() {
		return CompletableFuture.completedFuture(returnToFirstStep());
	}

	@RTMessageMapping("/step3")
	public CompletionStage<MessageResponse> step3(ExtendedMessage message, @RTContainerEntity UserInfo userInfo) {
		Integer peerId = message.getUserId();
		ObjectMapper objectMapper = new ObjectMapper();
		String level = message.getBody();
		if (level == null || (!level.equals("Бакалавриат | Специалитет") && !level.equals("Магистратура"))) {
			return returnToFirstStepCF();
		}
		return facultyService.getGroupsList(userInfo.getFacultyId())
				.thenApply(response -> {
					List<KeyboardButton> buttons = response.stream()
							.filter(gi -> gi.getLevel().equals(level))
							.map(GetGroupsResponse.GroupInfo::getCourse)
							.distinct()
							.sorted()
							.map(course -> new KeyboardButton(KeyboardButton.Color.SECONDARY,
									Integer.toString(course), objectMapper.createObjectNode()
									.put(ROUTE, "/configure/step4")
									.put("level", level)
									.toString()))
							.collect(Collectors.toList());
					return MessageResponse.builder()
							.message(stringProperties.getChooseCourse())
							.keyboard(messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})))
							.build();
				});
	}

	@RTMessageMapping("/step4")
	public CompletionStage<MessageResponse> step4(ExtendedMessage message, @RTContainerEntity UserInfo userInfo)
			throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		String course = message.getBody();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String level = payload.get("level").asText();
		if (course == null || !course.matches(courseRegex) || level == null ||
				(!level.equals("Бакалавриат | Специалитет") && !level.equals("Магистратура"))) {
			return returnToFirstStepCF();
		}
		return facultyService.getGroupsList(userInfo.getFacultyId())
				.thenApply(response -> {
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
											.put(ROUTE, "/configure/step5")
											.put("groupName", groupName)
											.toString()))
							.collect(Collectors.toList());
					return MessageResponse.builder()
							.message(stringProperties.getChooseGroup())
							.keyboard(messageOutput.createKeyboard(true, buttons.toArray(new KeyboardButton[]{})))
							.build();
				});
	}

	@RTMessageMapping("/step5")
	public CompletionStage<MessageResponse> step5(ExtendedMessage message, @RTContainerEntity UserInfo userInfo)
			throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String groupId = payload.get("groupName").asText();
		String facultyId = userInfo.getFacultyId();
		if (groupId == null || !groupId.matches(groupRegex)) {
			return returnToFirstStepCF();
		}
		return facultyService.getGroupsList(facultyId)
				.thenCompose(response -> {
					Optional<GetGroupsResponse.GroupInfo> optionalGroupInfo = response.stream()
							.filter(gi -> gi.getName().equals(groupId)).findAny();
					if (optionalGroupInfo.isEmpty()) {
						return returnToFirstStepCF();
					}
					userInfo.setGroupId(groupId);
					if (optionalGroupInfo.get().getSubgroups() > 0) {
						return scheduleService.findDifferenceBetweenSubgroups(facultyId, optionalGroupInfo.get().getName())
								.thenApply(diff -> {
									LessonCellMirror mirror1 = diff.getFirst();
									LessonCellMirror mirror2 = diff.getSecond();
									String outMessage = (mirror2 != null ? stringProperties.getChooseSubgroup() :
											stringProperties.getChooseSubgroupWindow()) + "\n" +
											(mirror1.getWeekSign() != WeekSign.ANY ? String.format("%s (%s)\n",
													mirror1.getDayOfWeek().getDisplayName(TextStyle.FULL,
															Locale.forLanguageTag("ru-RU")).toUpperCase(), mirror1.getWeekSign()) :
													mirror1.getDayOfWeek().getDisplayName(TextStyle.FULL,
															Locale.forLanguageTag("ru-RU")).toUpperCase() + "\n"
											) +
											commonUtils.convertLessonCell(mirror1, false, true, true, false, false);
									String keyboard = messageOutput.createKeyboard(true,
											new KeyboardButton(KeyboardButton.Color.POSITIVE, stringProperties.getYes(),
													objectMapper.createObjectNode()
															.put(ROUTE, "/configure/step6")
															.put("subgroup", diff.getFirstSubgroup())
															.toString()),
											new KeyboardButton(KeyboardButton.Color.NEGATIVE, stringProperties.getNo(),
													objectMapper.createObjectNode()
															.put(ROUTE, "/configure/step6")
															.put("subgroup", diff.getSecondSubgroup())
															.toString()));
									return MessageResponse.builder()
											.message(outMessage)
											.keyboard(keyboard)
											.build();
								});
					} else {
						return registerUser(message, userInfo);
					}
				});
	}

	@RTMessageMapping("/step6")
	public CompletionStage<MessageResponse> step6(ExtendedMessage message, @RTContainerEntity UserInfo userInfo) throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(message.getPayload());
		int subgroup = jsonNode.get("subgroup").asInt();
		userInfo.setSubgroup(subgroup);
		return registerUser(message, userInfo)
				.thenApplyAsync(mr ->
						MessageResponse.builder()
								.message(stringProperties.getSubgroupNotify())
								.build()
								.resolve(mr)
				);
	}

	private CompletionStage<MessageResponse> registerUser(ExtendedMessage message, UserInfo userInfo) {
		service.save(userInfo);
		log.info("Registered user {} with faculty {}, group {} and subgroup {}", message.getUserId(), userInfo.getFacultyId(), userInfo.getGroupId(), userInfo.getSubgroup());
		return CompletableFuture.completedFuture(MessageResponse.builder()
				.message(stringProperties.getSuccess())
				.set("userInfo", null)
				.redirectTo("/home")
				.build());
	}
}
