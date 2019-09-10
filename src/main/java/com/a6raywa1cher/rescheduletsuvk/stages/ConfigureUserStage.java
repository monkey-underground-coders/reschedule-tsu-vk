package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.dao.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.utils.VkKeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.ConfigureUserStage.NAME;

@Component(NAME)
public class ConfigureUserStage implements Stage {
	public static final String NAME = "configureUserStage";
	private VkApiClient vk;
	private GroupActor groupActor;
	private RtsServerRestComponent restComponent;
	private Map<Integer, Integer> step;
	private UserInfoService service;

	@Autowired
	public ConfigureUserStage(VkApiClient vk, GroupActor groupActor, RtsServerRestComponent restComponent,
	                          UserInfoService service) {
		this.vk = vk;
		this.groupActor = groupActor;
		this.restComponent = restComponent;
		this.service = service;
		this.step = new HashMap<>();
	}

	private void step1(ExtendedMessage message) {
		Integer peerId = message.getUserId();
		restComponent.getFaculties()
				.thenAccept(response -> {
					List<VkKeyboardButton> buttons = new ArrayList<>();
					ObjectMapper objectMapper = new ObjectMapper();
					for (String facultyId : response.getFaculties()) {
						buttons.add(new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, facultyId,
								objectMapper.createObjectNode()
										.put("button", "1")
										.put(ROUTE, NAME)
										.toString()));
					}
					VkUtils.sendMessage(vk, groupActor, message.getUserId(),
							"\u2b07\ufe0fВыбери свой, если он присутствует\u2b07\ufe0f",
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
					step.put(peerId, 2);
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
		restComponent.getGroups(facultyId)
				.thenAccept(response -> {
					ObjectMapper objectMapper = new ObjectMapper();
					List<VkKeyboardButton> buttons = response.getGroups().stream()
							.map(GetGroupsResponse.GroupInfo::getLevel)
							.distinct()
							.sorted()
							.map(level -> new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, level, objectMapper.createObjectNode()
									.put(ROUTE, NAME)
									.set("building", objectMapper.valueToTree(userInfo))
									.toString()))
							.collect(Collectors.toList());
					VkUtils.sendMessage(vk, groupActor, message.getUserId(),
							"\u2b07\ufe0fХорошо, теперь выбери, где ты учишься\u2b07\ufe0f",
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
					step.put(peerId, 3);
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
		restComponent.getGroups(facultyId)
				.thenAccept(response -> {
					List<VkKeyboardButton> buttons = response.getGroups().stream()
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
							"\u2b07\ufe0fОкей, теперь курс?\u2b07\ufe0f",
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
					step.put(peerId, 4);
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
		restComponent.getGroups(facultyId)
				.thenAccept(response -> {
					List<VkKeyboardButton> buttons = response.getGroups().stream()
							.filter(gi -> gi.getLevel().equals(level))
							.filter(gi -> gi.getCourse().equals(Integer.parseInt(course)))
							.map(GetGroupsResponse.GroupInfo::getName)
							.distinct()
							.sorted()
							.map(groupName -> new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY,
									groupName, objectMapper.createObjectNode()
									.put(ROUTE, NAME)
									.put("level", level)
									.put("course", course)
									.set("building", payload.get("building"))
									.toString()))
							.collect(Collectors.toList());
					VkUtils.sendMessage(vk, groupActor, message.getUserId(),
							"\u2b07\ufe0fПрекрасно, последний шаг. Твоя группа?\u2b07\ufe0f",
							VkUtils.createKeyboard(true, buttons.toArray(new VkKeyboardButton[]{})));
					step.put(peerId, 5);
				});
	}

	private void step5(ExtendedMessage message) throws JsonProcessingException {
		Integer peerId = message.getUserId();
		if (message.getPayload() == null) {
			step.put(peerId, 1);
			return;
		}
		ObjectMapper objectMapper = new ObjectMapper();
		String groupId = message.getBody();
		JsonNode payload = objectMapper.readTree(message.getPayload());
		String facultyId = payload.get("building").get("facultyId").asText();
		restComponent.getGroups(facultyId)
				.thenAccept(response -> {
					Optional<GetGroupsResponse.GroupInfo> optionalGroupInfo = response.getGroups().stream()
							.filter(gi -> gi.getName().equals(groupId)).findAny();
					if (optionalGroupInfo.isEmpty()) {
						step1(message);
						return;
					}
					UserInfo userInfo = new UserInfo();
					userInfo.setPeerId(message.getUserId());
					userInfo.setFacultyId(facultyId);
					userInfo.setGroupId(groupId);
					service.save(userInfo);
					VkUtils.sendMessage(vk, groupActor, message.getUserId(), "Всё настроено!");
					step.remove(peerId);
				});
	}

	@Override
	public void accept(ExtendedMessage message) {
		Integer peerId = message.getUserId();
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
			}
		} catch (JsonProcessingException e) {
			step1(message);
		}
	}
}
