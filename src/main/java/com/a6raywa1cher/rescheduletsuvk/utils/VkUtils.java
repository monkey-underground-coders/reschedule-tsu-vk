package com.a6raywa1cher.rescheduletsuvk.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class VkUtils {
	private static final Logger log = LoggerFactory.getLogger(VkUtils.class);

	public static void sendMessage(VkApiClient vk, GroupActor group, Integer peerId, String message) {
		sendMessage(vk, group, peerId, message, null);
	}

	public static void sendMessage(VkApiClient vk, GroupActor group, Integer peerId, String message, String keyboard) {
		if (keyboard == null) keyboard = "{\"buttons\":[],\"one_time\":true}"; // clear keyboard
		try {
			if (!keyboard.equals("")) {
				vk.messages().send(group)
						.message(message)
						.peerId(peerId)
						.unsafeParam("dont_parse_links", 1)
						.unsafeParam("keyboard", keyboard)
						.execute();
			} else {
				vk.messages().send(group)
						.message(message)
						.peerId(peerId)
						.unsafeParam("dont_parse_links", 1)
						.execute();
			}
		} catch (ApiException | ClientException e) {
			log.error("Message not sent", e);
		}
	}

	public static String createKeyboard(boolean oneTime, VkKeyboardButton... buttonsDescriptions) {
		return createKeyboard(oneTime, null, buttonsDescriptions);
	}

	public static String createKeyboard(boolean oneTime, int[] grid, VkKeyboardButton... buttonsDescriptions) {
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode allButtons = objectMapper.createArrayNode();
		ArrayNode buttonsRaw = objectMapper.createArrayNode();
		if (grid == null) {
			int size = buttonsDescriptions.length;
			int maxWidth, maxHeight;
			if (16 < size) {
				maxWidth = 4;
				maxHeight = 17;
			} else if (9 < size) {
				maxWidth = 4;
				maxHeight = 4;
			} else if (4 < size) {
				maxWidth = 3;
				maxHeight = 3;
			} else {
				maxWidth = 2;
				maxHeight = 2;
			}
			int fullColumns = size / maxHeight;
			if (fullColumns > maxWidth) {
				throw new IllegalArgumentException("Too many data!");
			}
			int remaining = size % maxHeight;
			grid = new int[fullColumns == 0 ? remaining : maxHeight];
			int baseFill = fullColumns;
			int extendedFill = baseFill + 1;
			Arrays.fill(grid, remaining, grid.length, baseFill);
			Arrays.fill(grid, 0, remaining, extendedFill);
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
