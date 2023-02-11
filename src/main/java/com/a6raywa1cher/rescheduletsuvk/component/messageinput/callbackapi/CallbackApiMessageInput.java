package com.a6raywa1cher.rescheduletsuvk.component.messageinput.callbackapi;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageinput.MessageInput;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageRouter;
import com.a6raywa1cher.rescheduletsuvk.config.vkendpoint.VkConfigProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
public class CallbackApiMessageInput implements MessageInput {
	private static final Logger log = LoggerFactory.getLogger(CallbackApiMessageInput.class);
	private MessageRouter component;
	private ObjectMapper objectMapper;
	private VkConfigProperties properties;

	public CallbackApiMessageInput(MessageRouter component, VkConfigProperties properties) {
		this.component = component;
		this.properties = properties;
		this.objectMapper = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@PostMapping("/callback")
	public ResponseEntity<?> callbackRequest(@RequestBody @Validated CallbackApiInput json) throws JsonProcessingException {
		log.info("Got callback req");
		if (!json.getSecret().equals(properties.getSecretKey()) || !json.getGroupId().equals(properties.getGroupId())) {
			log.error("Invalid secret!");
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (json.getType().equals("confirmation")) {
			log.info("Confirmation req got");
			return ResponseEntity.ok().body(properties.getSecretConfirm());
		}
		if (!json.getType().equals("message_new")) {
			log.info("Unknown event");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
		try {
			component.routeMessage(objectMapper.treeToValue(json.getObject(), ExtendedMessage.class));
		} catch (Exception e) {
			Sentry.capture(e);
			log.error("Exception during routing", e);
			throw e;
		}
		return ResponseEntity.ok().body("ok");
	}

	@Override
	public void run() throws Exception {

	}
}
