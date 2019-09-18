package com.a6raywa1cher.rescheduletsuvk.component;

import com.a6raywa1cher.rescheduletsuvk.stages.PrimaryStage;
import com.a6raywa1cher.rescheduletsuvk.stages.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StageRouterComponent {
	public static final String ROUTE = "route";
	private static final Logger log = LoggerFactory.getLogger(StageRouterComponent.class);
	private final Map<String, ? extends Stage> stageMap;
	private final Map<Integer, Stage> hardlinkMap;
	private final PrimaryStage primaryStage;
	private final VkApiClient vk;
	private final GroupActor groupActor;

	@Autowired
	public StageRouterComponent(VkApiClient vk, GroupActor groupActor,
	                            @Lazy Map<String, ? extends Stage> stageMap, @Lazy PrimaryStage primaryStage) {
		this.stageMap = stageMap;
		this.primaryStage = primaryStage;
		this.vk = vk;
		this.groupActor = groupActor;
		this.hardlinkMap = new ConcurrentHashMap<>();
	}

	@PostConstruct
	public void onStart() throws ClientException, ApiException {
		vk.groups().setLongPollSettings(groupActor).enabled(true)
				.messageNew(true)
				.execute();
	}

	public void link(Integer peerId, Stage stage) {
		hardlinkMap.put(peerId, stage);
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
		if (message.getPayload() != null) {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				JsonNode jsonNode = objectMapper.readTree(message.getPayload());
				log.debug("Routing user {} message with payload to " + jsonNode.get(ROUTE).asText(), message.getUserId());
				stageMap.get(jsonNode.get(ROUTE).asText()).accept(message);
			} catch (Exception e) {
				log.debug("Failover routing user {} to primaryStage", message.getUserId());
				primaryStage.accept(message);
			}
		} else {
			log.debug("Routing payload-blank message from {} to {}", message.getUserId(),
					hardlinkMap.getOrDefault(message.getUserId(), primaryStage).getClass().getName());
			hardlinkMap.getOrDefault(message.getUserId(), primaryStage).accept(message);
		}
	}
}
