package com.a6raywa1cher.rescheduletsuvk.component.router;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.filterstages.FilterStage;
import com.a6raywa1cher.rescheduletsuvk.stages.PrimaryStage;
import com.a6raywa1cher.rescheduletsuvk.stages.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@Component
public class DefaultMessageRouter implements MessageRouter {
	private static final Logger log = LoggerFactory.getLogger(DefaultMessageRouter.class);
	private final Map<String, Stage> stageMap;
	private final Map<Integer, Stage> hardlinkMap;
	private final List<FilterStage> filterStages;
	private PrimaryStage primaryStage;
	private final Executor executor;

	@Autowired
	public DefaultMessageRouter() {
		this.stageMap = new HashMap<>();
		this.filterStages = new ArrayList<>();
		this.primaryStage = null;
		this.hardlinkMap = new ConcurrentHashMap<>();
		executor = new ForkJoinPool();
	}

	// true if new link
	public boolean link(Integer peerId, Stage stage) {
		return hardlinkMap.put(peerId, stage) == null;
	}

	public void unlink(Integer peerId) {
		hardlinkMap.remove(peerId);
	}

	public void routeMessageTo(ExtendedMessage message, String stage) {
		if (stageMap.containsKey(stage)) {
			stageMap.get(stage).accept(message);
		} else {
			throw new IllegalArgumentException("No such route");
		}
	}

	@Override
	public void addStage(Stage stage, String name) {
		this.stageMap.put(name, stage);
		log.info("Putted stage {}", name);
	}

	@Override
	public void addFilter(FilterStage filterStage) {
		this.filterStages.add(filterStage);
		log.info("Putted filter {}", filterStage.getClass().getSimpleName());
	}

	@Override
	public void setPrimaryStage(PrimaryStage primaryStage) {
		this.primaryStage = primaryStage;
		log.info("Setted primary stage {}", primaryStage.getClass().getSimpleName());
	}

	public void routeMessage(ExtendedMessage message) {
		executor.execute(() -> $routeMessage(message));
	}

	private void $routeMessage(ExtendedMessage message) {
		try {
			int userId = message.getUserId();
			for (FilterStage filterStage : filterStages) {
				message = filterStage.process(message);
				if (message == null) {
					log.info("Global listener {} stopped processing of {}'s message",
							filterStage.getClass().toString(), userId);
					return;
				}
			}
			if (message.getPayload() != null) {
				ObjectMapper objectMapper = new ObjectMapper();
				try {
					JsonNode jsonNode = objectMapper.readTree(message.getPayload());
					log.debug("Routing user {} message with payload to " + jsonNode.get(ROUTE).asText(), userId);
					stageMap.get(jsonNode.get(ROUTE).asText()).accept(message);
				} catch (Exception e) {
					log.debug("Failover routing user {} to primaryStage", userId);
					primaryStage.accept(message);
				}
			} else {
				log.debug("Routing payload-blank message from {} to {}", userId,
						hardlinkMap.getOrDefault(userId, primaryStage).getClass().getName());
				hardlinkMap.getOrDefault(userId, primaryStage).accept(message);
			}
		} catch (Exception e) {
			log.error("Catched exception", e);
			Sentry.capture(e);
		}
	}
}
