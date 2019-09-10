package com.a6raywa1cher.rescheduletsuvk.component;

import com.a6raywa1cher.rescheduletsuvk.stages.PrimaryStage;
import com.a6raywa1cher.rescheduletsuvk.stages.Stage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

@Component
public class StageRouterComponent {
	public static final String ROUTE = "route";
	private final Map<String, ? extends Stage> stageMap;
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
	}

	@PostConstruct
	public void onStart() throws ClientException, ApiException {
		vk.groups().setLongPollSettings(groupActor).enabled(true)
				.messageNew(true)
				.execute();
	}

	public void startListening() {
		CallbackApiLongPollHandler handler = new CallbackApiLongPollHandler(vk, groupActor, this);
		while (true) {
			try {
				handler.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void routeMessage(ExtendedMessage message) {
		if (message.getPayload() != null) {
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				JsonNode jsonNode = objectMapper.readTree(message.getPayload());
				stageMap.get(jsonNode.get(ROUTE).asText()).accept(message);
			} catch (JsonProcessingException e) {
				primaryStage.accept(message);
			}
		} else {
			primaryStage.accept(message);
		}
	}
}
