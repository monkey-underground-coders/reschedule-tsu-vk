package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.FacultyService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.VkKeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.ConfigureUserStage.NAME;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.ARROW_DOWN_EMOJI;

@Component(NAME)
public class ConfigureUserStage implements Stage {
	public static final String NAME = "configureUserStage";
	private static final Logger log = LoggerFactory.getLogger(ConfigureUserStage.class);
	private VkApiClient vk;
	private GroupActor groupActor;
	private RtsServerRestComponent restComponent;
	private Map<Integer, Integer> step;
	private UserInfoService service;
	private StageRouterComponent component;
	private AppConfigProperties properties;
	private ScheduleService scheduleService;
	private FacultyService facultyService;

	@Autowired
	public ConfigureUserStage(VkApiClient vk, GroupActor groupActor, RtsServerRestComponent restComponent,
	                          UserInfoService service, StageRouterComponent component, AppConfigProperties properties,
	                          ScheduleService scheduleService, FacultyService facultyService) {
		this.vk = vk;
		this.groupActor = groupActor;
		this.restComponent = restComponent;
		this.service = service;
		this.component = component;
		this.properties = properties;
		this.scheduleService = scheduleService;
		this.facultyService = facultyService;
		this.step = new HashMap<>();
	}

	private void step1(ExtendedMessage message) {
		Integer peerId = message.getUserId();
		facultyService.getFacultiesList()
				.thenAccept(response -> {
					List<VkKeyboardButton> buttons = new ArrayList<>();
					ObjectMapper objectMapper = new ObjectMapper();
					response.stream()
							.sorted()
							.forEach(facultyId -> {
								boolean red = properties.getRedFaculties().contains(facultyId);
								buttons.add(new VkKeyboardButton(
										red ? VkKeyboardButton.Color.NEGATIVE : VkKeyboardButton.Color.SECONDARY,
										facultyId,
										objectMapper.createObjectNode()
												.put("button", "1")
												.put(ROUTE, NAME)
												.toString()));
							});
					VkUtils.sendMessage(vk, groupActor, message.getUserId(),
							"Если факультет - красный, то, возможно, у него некорректное расписание\n" +
									ARROW_DOWN_EMOJI + "Выбери свой, если он присутствует" + ARROW_DOWN_EMOJI,
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
					step.put(peerId, 2);
				})
				.exceptionally(e -> {
					log.error("step 1 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step2(ExtendedMessage message) {
		Integer peerId = message.getUserId();
		if (message.getPayload() == null) {
			step.put(peerId, 1);
			return;
		}
		String facultyId = message.getBody();
		UserInfo userInfo = new UserInfo();
		userInfo.setPeerId(peerId);
		userInfo.setFacultyId(facultyId);
		facultyService.getGroupsList(facultyId)
				.thenAccept(response -> {
					ObjectMapper objectMapper = new ObjectMapper();
					List<VkKeyboardButton> buttons = response.stream()
							.map(GetGroupsResponse.GroupInfo::getLevel)
							.distinct()
							.sorted()
							.map(level -> new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, level, objectMapper.createObjectNode()
									.put(ROUTE, NAME)
									.set("building", objectMapper.valueToTree(userInfo))
									.toString()))
							.collect(Collectors.toList());
					VkUtils.sendMessage(vk, groupActor, message.getUserId(),
							ARROW_DOWN_EMOJI + "Хорошо, теперь выбери, где ты учишься" + ARROW_DOWN_EMOJI,
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
					step.put(peerId, 3);
				})
				.exceptionally(e -> {
					log.error("step 2 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step3(ExtendedMessage message) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		if (message.getPayload() == null) {
			step.put(peerId, 1);
			return;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		String level = message.getBody();
		JsonNode info = objectMapper.readTree(message.getPayload());
		String facultyId = info.get("building").get("facultyId").asText();
		facultyService.getGroupsList(facultyId)
				.thenAccept(response -> {
					List<VkKeyboardButton> buttons = response.stream()
							.filter(gi -> gi.getLevel().equals(level))
							.map(GetGroupsResponse.GroupInfo::getCourse)
							.distinct()
							.sorted()
							.map(course -> new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY,
									Integer.toString(course), objectMapper.createObjectNode()
									.put(ROUTE, NAME)
									.put("level", level)
									.set("building", info.get("building"))
									.toString()))
							.collect(Collectors.toList());
					VkUtils.sendMessage(vk, groupActor, message.getUserId(),
							ARROW_DOWN_EMOJI + "Окей, теперь курс?" + ARROW_DOWN_EMOJI,
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
					step.put(peerId, 4);
				})
				.exceptionally(e -> {
					log.error("step 3 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step4(ExtendedMessage message) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		if (message.getPayload() == null) {
			step.put(peerId, 1);
			return;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		String course = message.getBody();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String facultyId = payload.get("building").get("facultyId").asText();
		String level = payload.get("level").asText();
		facultyService.getGroupsList(facultyId)
				.thenAccept(response -> {
					List<VkKeyboardButton> buttons = response.stream()
							.filter(gi -> gi.getLevel().equals(level))
							.filter(gi -> gi.getCourse().equals(Integer.parseInt(course)))
							.map(GetGroupsResponse.GroupInfo::getName)
							.distinct()
							.sorted()
							.map(groupName -> new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY,
									groupName.length() >= 40 ? groupName.substring(0, 20) + "..." +
											groupName.substring(groupName.length() - 15) : groupName,
									objectMapper.createObjectNode()
											.put(ROUTE, NAME)
											.put("level", level)
											.put("course", course)
											.put("groupName", groupName)
											.set("building", payload.get("building"))
											.toString()))
							.collect(Collectors.toList());
					VkUtils.sendMessage(vk, groupActor, message.getUserId(),
							ARROW_DOWN_EMOJI + "Прекрасно, последний шаг. Твоя группа?" + ARROW_DOWN_EMOJI,
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
					step.put(peerId, 5);
				})
				.exceptionally(e -> {
					log.error("step 4 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step5(ExtendedMessage message) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		if (message.getPayload() == null) {
			step.put(peerId, 1);
			return;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String facultyId = payload.get("building").get("facultyId").asText();
		String groupId = payload.get("groupName").asText();
		facultyService.getGroupsList(facultyId)
				.thenCompose(response -> {
					Optional<GetGroupsResponse.GroupInfo> optionalGroupInfo = response.stream()
							.filter(gi -> gi.getName().equals(groupId)).findAny();
					if (optionalGroupInfo.isEmpty()) {
						step1(message);
						return CompletableFuture.completedFuture(null);
					}
					if (optionalGroupInfo.get().getSubgroups() > 0) {
						return scheduleService.findDifferenceBetweenSubgroups(facultyId, optionalGroupInfo.get().getName())
								.thenAccept(diff -> {
									LessonCellMirror mirror1 = diff.getFirst();
									LessonCellMirror mirror2 = diff.getSecond();
									VkUtils.sendMessage(vk, groupActor, message.getUserId(),
											"Упс, не последний. Известно, что у этой группы есть подгруппы.\n" +
													"Есть ли у тебя вот эта пара?" + (mirror2 != null ? "" : "(у другой подгруппы окно)") + "\n" +
													String.format("%s (%s)",
															mirror1.getDayOfWeek().getDisplayName(TextStyle.FULL,
																	Locale.forLanguageTag("ru-RU")),
															mirror1.getWeekSign().getPrettyString()) +
													CommonUtils.convertLessonCell(mirror1, false, true),
											VkUtils.createKeyboard(true,
													new VkKeyboardButton(VkKeyboardButton.Color.POSITIVE, "Да",
															objectMapper.createObjectNode()
																	.put(ROUTE, NAME)
																	.put("subgroup", diff.getFirstSubgroup())
																	.put("groupId", groupId)
																	.put("facultyId", facultyId)
																	.toString()),
													new VkKeyboardButton(VkKeyboardButton.Color.NEGATIVE, "Нет",
															objectMapper.createObjectNode()
																	.put(ROUTE, NAME)
																	.put("subgroup", diff.getSecondSubgroup())
																	.put("groupId", groupId)
																	.put("facultyId", facultyId)
																	.toString()))
									);
									step.put(message.getUserId(), 6);
								});
					} else {
						registerUser(message, message.getUserId(), facultyId, groupId);
						return CompletableFuture.completedFuture(null);
					}
				})
				.exceptionally(e -> {
					log.error("step 5 error", e);
					Sentry.capture(e);
					return null;
				});
	}

	private void step6(ExtendedMessage message) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		if (message.getPayload() == null) {
			step.put(peerId, 1);
			return;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(message.getPayload());
		int subgroup = jsonNode.get("subgroup").asInt();
		VkUtils.sendMessage(vk, groupActor, message.getUserId(),
				"Твоя подгруппа:" + subgroup);
		String groupId = jsonNode.get("groupId").asText();
		String facultyId = jsonNode.get("facultyId").asText();
		registerUser(message, message.getUserId(), facultyId, groupId, subgroup);
	}

	private void registerUser(ExtendedMessage message, Integer peerId, String facultyId, String groupId) {
		registerUser(message, peerId, facultyId, groupId, null);
	}

	private void registerUser(ExtendedMessage message, Integer peerId, String facultyId, String groupId, Integer subgroup) {
		UserInfo userInfo = new UserInfo();
		userInfo.setPeerId(peerId);
		userInfo.setFacultyId(facultyId);
		userInfo.setGroupId(groupId);
		userInfo.setSubgroup(subgroup);
		service.save(userInfo);
		VkUtils.sendMessage(vk, groupActor, peerId, "Всё настроено!");
		step.remove(peerId);
		component.routeMessage(message, MainMenuStage.NAME);
	}

	@Override
	public void accept(ExtendedMessage message) {
		Integer peerId = message.getUserId();
		if (message.getPayload() == null) step.put(peerId, 1);
		step.putIfAbsent(peerId, 1);
		try {
			switch (step.get(peerId)) {
				case 1:
					step1(message);
					break;
				case 2:
					step2(message);
					break;
				case 3:
					step3(message);
					break;
				case 4:
					step4(message);
					break;
				case 5:
					step5(message);
					break;
				case 6:
					step6(message);
					break;
			}
		} catch (JsonProcessingException e) {
			step1(message);
		}
	}
}
