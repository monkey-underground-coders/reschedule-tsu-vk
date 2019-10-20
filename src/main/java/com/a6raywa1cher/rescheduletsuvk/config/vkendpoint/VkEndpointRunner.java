package com.a6raywa1cher.rescheduletsuvk.config.vkendpoint;

import com.a6raywa1cher.rescheduletsuvk.component.messageinput.MessageInput;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class VkEndpointRunner {
	private static final Logger log = LoggerFactory.getLogger(VkEndpointRunner.class);

	private MessageInput messageInput;

	public VkEndpointRunner(MessageInput messageInput) {
		this.messageInput = messageInput;
	}

	@Scheduled(fixedDelay = 200)
	public void run() {
		log.info("Invoking messageInput.run() ...");
		try {
			messageInput.run();
		} catch (Exception e) {
			Sentry.capture(e);
			log.error("Listening error", e);
		}
	}
}
