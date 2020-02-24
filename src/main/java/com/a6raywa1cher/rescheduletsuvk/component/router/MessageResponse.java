package com.a6raywa1cher.rescheduletsuvk.component.router;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public class MessageResponse {
	private List<String> messages;

	private Integer to;

	private String keyboard;

	private String redirectTo;

	private String textQueryParserPath;

	private Map<String, Object> containerChanges;

	private MessageResponse() {
	}

	public static MessageResponseBuilder builder() {
		return new MessageResponseBuilder();
	}

	public MessageResponse resolve(MessageResponse other) {
		MessageResponse out = new MessageResponse();
		out.messages = new LinkedList<>();
		out.messages.addAll(this.getMessages());
		out.messages.addAll(other.getMessages());
		out.to = other.to != null ? other.to : this.to;
		out.keyboard = other.keyboard != null ? other.keyboard : this.keyboard;
		out.redirectTo = other.redirectTo != null ? other.redirectTo : this.keyboard;
		out.textQueryParserPath = other.textQueryParserPath != null ? other.textQueryParserPath : this.textQueryParserPath;
		out.containerChanges = new HashMap<>();
		out.containerChanges.putAll(this.containerChanges);
		out.containerChanges.putAll(other.containerChanges);
		return out;
	}

	public static class MessageResponseBuilder {
		private List<String> message = new LinkedList<>();
		private Integer to;
		private String keyboard;
		private String redirectTo;
		private String textQueryParserPath;
		private Map<String, Object> containerChanges = new HashMap<>();

		MessageResponseBuilder() {
		}

		public <T> MessageResponseBuilder set(String label, T obj) {
			containerChanges.put(label, obj);
			return this;
		}

		public MessageResponseBuilder message(String message) {
			this.message.add(message);
			return this;
		}

		public MessageResponseBuilder to(Integer to) {
			this.to = to;
			return this;
		}

		public MessageResponseBuilder keyboard(String keyboard) {
			this.keyboard = keyboard;
			return this;
		}

		public MessageResponseBuilder redirectTo(String redirectTo) {
			this.redirectTo = redirectTo;
			return this;
		}

		public MessageResponseBuilder textQueryParserPath(String textQueryParserPath) {
			this.textQueryParserPath = textQueryParserPath;
			return this;
		}

		public MessageResponse build() {
			return new MessageResponse(message, to, keyboard, redirectTo, textQueryParserPath, containerChanges);
		}
	}
}
