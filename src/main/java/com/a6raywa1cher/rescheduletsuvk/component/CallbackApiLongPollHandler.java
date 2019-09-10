package com.a6raywa1cher.rescheduletsuvk.component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vk.api.sdk.callback.longpoll.CallbackApiLongPoll;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;

public class CallbackApiLongPollHandler extends CallbackApiLongPoll {
	private StageRouterComponent component;
	private Gson gson;

	public CallbackApiLongPollHandler(VkApiClient client, GroupActor actor, StageRouterComponent component) {
		super(client, actor);
		this.component = component;
		this.gson = new Gson();
	}

	@Override
	public boolean parse(JsonObject json) {
		try {
			String type = json.get("type").getAsString();
			if (!type.equals("message_new")) return super.parse(json);
			String info = json.get("object").toString();
			component.routeMessage(gson.fromJson(info, ExtendedMessage.class));
			return true;
		} catch (Exception e) {
			return super.parse(json);
		}
	}
//	@Override
//	public boolean parse(String json) {
//		return super.parse(json);
//	}
//
//	@Override
//	public void messageNew(Integer groupId, Message message) {
//		component.routeMessage(groupId, message);
//	}
}
