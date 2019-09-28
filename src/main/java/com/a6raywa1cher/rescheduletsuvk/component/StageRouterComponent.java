package com.a6raywa1cher.rescheduletsuvk.component;

import com.a6raywa1cher.rescheduletsuvk.filterstages.FilterStage;
import com.a6raywa1cher.rescheduletsuvk.stages.PrimaryStage;
import com.a6raywa1cher.rescheduletsuvk.stages.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

@Component
public class StageRouterComponent {
	public static final String ROUTE = "route";
	private static final Logger log = LoggerFactory.getLogger(StageRouterComponent.class);
	private final Map<String, ? extends Stage> stageMap;
	private final Map<Integer, Stage> hardlinkMap;
	private final List<? extends FilterStage> globalListeners;
	private final PrimaryStage primaryStage;
	private final VkApiClient vk;
	private final GroupActor groupActor;
	private final Executor executor;

	@Autowired
	public StageRouterComponent(VkApiClient vk, GroupActor groupActor,
	                            @Lazy Map<String, ? extends Stage> stageMap,
	                            @Lazy List<? extends FilterStage> globalListeners,
	                            @Lazy PrimaryStage primaryStage) {
		this.stageMap = stageMap;
		this.globalListeners = globalListeners;
		this.primaryStage = primaryStage;
		this.vk = vk;
		this.groupActor = groupActor;
		this.hardlinkMap = new ConcurrentHashMap<>();
		executor = new ForkJoinPool();
	}

	@PostConstruct
	public void onStart() throws ClientException, ApiException {
		vk.groups().setLongPollSettings(groupActor).enabled(true)
				.messageNew(true)
				.execute();
	}

	// true if new link
	public boolean link(Integer peerId, Stage stage) {
		return hardlinkMap.put(peerId, stage) == null;
	}

	public void unlink(Integer peerId) {
		hardlinkMap.remove(peerId);
	}

	public void startListening() {
		CallbackApiLongPollHandler handler = new CallbackApiLongPollHandler(vk, groupActor, this);
		while (true) {
			try {
				handler.run();
			} catch (Exception e) {
				Sentry.capture(e);
				log.error("Listening error", e);
			}
		}
	}

	public void routeMessage(ExtendedMessage message, String stage) {
		if (stageMap.containsKey(stage)) {
			stageMap.get(stage).accept(message);
		} else {
			throw new IllegalArgumentException("No such route");
		}
	}

	public void routeMessage(ExtendedMessage message) {
		executor.execute(() -> $routeMessage(message));
	}

	private void $routeMessage(ExtendedMessage message) {
		int userId = message.getUserId();
		for (FilterStage filterStage : globalListeners) {
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
	}
}
