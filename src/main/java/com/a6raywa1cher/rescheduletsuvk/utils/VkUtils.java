package com.a6raywa1cher.rescheduletsuvk.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.petersamokhin.bots.sdk.clients.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class VkUtils {
	private static final Logger log = LoggerFactory.getLogger(VkUtils.class);

	public static void sendMessage(Group group, Integer peerId, String message) {
		sendMessage(group, peerId, message, null);
	}

	public static void sendMessage(Group group, Integer peerId, String message, String keyboard) {
		if (keyboard == null) keyboard = "{\"buttons\":[],\"one_time\":true}"; // clear keyboard
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectNode params = objectMapper.createObjectNode()
				.put("message", message)
				.put("peer_id", peerId)
				.put("keyboard", keyboard);
		group.api().call("messages.send", params.toString(), response -> {
			if (!(response instanceof Integer)) {
				log.error("Message not sent: {}", response);
			}
		});
	}

	public static String createKeyboard(boolean oneTime, VkKeyboardButton... buttonsDescriptions) {
		return createKeyboard(oneTime, null, buttonsDescriptions);
	}

	public static String createKeyboard(boolean oneTime, int[] grid, VkKeyboardButton... buttonsDescriptions) {
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode allButtons = objectMapper.createArrayNode();
		ArrayNode buttonsRaw = objectMapper.createArrayNode();
		if (grid == null) {
			int fullRaws = buttonsDescriptions.length / 4;
			int remaining = buttonsDescriptions.length % 4;
			grid = new int[fullRaws != 0 ? 4 : remaining];
			Arrays.fill(grid, fullRaws);
			if (remaining > 0) Arrays.fill(grid, 0, remaining, fullRaws + 1);
		}
		int currRaw = 0;
		int posInRaw = 0;
		for (VkKeyboardButton vkKeyboardButton : buttonsDescriptions) {
			ObjectNode button = objectMapper.createObjectNode();
			button.put("color", vkKeyboardButton.getColor().name().toLowerCase());
			ObjectNode action = objectMapper.createObjectNode()
					.put("type", "text")
					.put("label", vkKeyboardButton.getLabel());
			if (vkKeyboardButton.getPayload() != null) {
				action.put("payload", vkKeyboardButton.getPayload());
			}
			button.set("action", action);
			buttonsRaw.add(button);
			posInRaw++;
			if (posInRaw == grid[currRaw]) {
				allButtons.add(buttonsRaw);
				buttonsRaw = objectMapper.createArrayNode();
				currRaw++;
				posInRaw = 0;
			}
		}
		return objectMapper.createObjectNode()
				.put("one_time", oneTime)
				.set("buttons", allButtons)
				.toString();
	}
}
