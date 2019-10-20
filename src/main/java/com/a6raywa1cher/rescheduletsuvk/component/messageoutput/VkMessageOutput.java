package com.a6raywa1cher.rescheduletsuvk.component.messageoutput;

import com.a6raywa1cher.rescheduletsuvk.utils.KeyboardButton;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

public class VkMessageOutput implements MessageOutput {
	private static final Logger log = LoggerFactory.getLogger(VkMessageOutput.class);
	private VkApiClient vk;
	private GroupActor group;

	@Autowired
	public VkMessageOutput(VkApiClient vk, GroupActor group) {
		this.vk = vk;
		this.group = group;
	}

	public <T> void sendMessage(T peerId, String message) {
		sendMessage(peerId, message, null);
	}

	public <T> void sendMessage(T peerId, String message, String keyboard) {
		if (keyboard == null) keyboard = "{\"buttons\":[],\"one_time\":true}"; // clear keyboard
		if (!(peerId instanceof Integer)) {
			throw new IllegalArgumentException("Not integer peerId");
		}
		try {
			if (!keyboard.equals("")) {
				vk.messages().send(group)
						.message(message)
						.peerId((Integer) peerId)
						.unsafeParam("dont_parse_links", 1)
						.unsafeParam("keyboard", keyboard)
						.execute();
			} else {
				vk.messages().send(group)
						.message(message)
						.peerId((Integer) peerId)
						.unsafeParam("dont_parse_links", 1)
						.execute();
			}
		} catch (ApiException | ClientException e) {
			throw new RuntimeException("Message execution failed", e);
		}
	}

	public String createKeyboard(boolean oneTime, KeyboardButton... buttonsDescriptions) {
		return createKeyboard(oneTime, null, buttonsDescriptions);
	}

	private String truncate(String str) {
		if (str.length() < 30) {
			return str;
		}
		return str.substring(0, 12) + "..." + str.substring(str.length() - 12);
	}

	public String createKeyboard(boolean oneTime, int[] grid, KeyboardButton... buttonsDescriptions) {
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode allButtons = objectMapper.createArrayNode();
		ArrayNode buttonsRaw = objectMapper.createArrayNode();
		if (grid == null) {
			int size = buttonsDescriptions.length;
			int maxWidth, maxHeight;
			if (16 < size) {
				maxWidth = 4;
				maxHeight = 10;
//				maxHeight = 17;
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
		for (KeyboardButton keyboardButton : buttonsDescriptions) {
			ObjectNode button = objectMapper.createObjectNode();
			// groupName.length() >= 40 ? groupName.substring(0, 20) + "..." +
			// groupName.substring(groupName.length() - 15) : groupName,
			button.put("color", keyboardButton.getColor().name().toLowerCase());
			String label = keyboardButton.getLabel();
			if (label.length() >= 40) {
				label = label.substring(0, 20) + "..." + label.substring(label.length() - 15);
			}
			ObjectNode action = objectMapper.createObjectNode()
					.put("type", "text")
					.put("label", truncate(keyboardButton.getLabel()));
			if (keyboardButton.getPayload() != null) {
				action.put("payload", keyboardButton.getPayload());
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

	@Override
	public String getDefaultPayload() {
		return "{\"buttons\": \"1\"}";
	}
}
