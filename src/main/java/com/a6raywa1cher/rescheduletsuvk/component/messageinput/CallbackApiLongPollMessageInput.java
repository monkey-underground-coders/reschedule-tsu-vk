package com.a6raywa1cher.rescheduletsuvk.component.messageinput;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.config.vkendpoint.VkConfigProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.vk.api.sdk.callback.longpoll.CallbackApiLongPoll;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class CallbackApiLongPollMessageInput extends CallbackApiLongPoll implements MessageInput {
	private static final Logger log = LoggerFactory.getLogger(CallbackApiLongPollMessageInput.class);
	private MessageRouter component;
	private VkApiClient vk;
	private GroupActor group;
	private ObjectMapper objectMapper;
	private VkConfigProperties properties;

	@Autowired
	public CallbackApiLongPollMessageInput(VkApiClient client, GroupActor actor, MessageRouter component, VkConfigProperties properties) {
		super(client, actor);
		this.component = component;
		this.vk = client;
		this.group = actor;
		this.properties = properties;
		this.objectMapper = new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@PostConstruct
	public void onStart() throws ClientException, ApiException {
		vk.groups().setLongPollSettings(group, properties.getGroupId()).enabled(true)
				.messageNew(true)
				.execute();
	}

	@Override
	public boolean parse(JsonObject json) {
		try {
			String type = json.get("type").getAsString();
			if (!type.equals("message_new")) return super.parse(json);
			String info = json.get("object").toString();
			component.routeMessage(objectMapper.readValue(info, ExtendedMessage.class));
			return true;
		} catch (Exception e) {
			Sentry.capture(e);
			log.error("Parse exception", e);
			return super.parse(json);
		}
	}
}
