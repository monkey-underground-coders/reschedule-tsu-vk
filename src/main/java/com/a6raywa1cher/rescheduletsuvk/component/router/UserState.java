package com.a6raywa1cher.rescheduletsuvk.component.router;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class UserState {
	private final int userId;

	private String textQueryConsumerPath;

	private Map<String, Object> container = new HashMap<>();

	@Override
	public String toString() {
		return "UserState{" +
			"userId=" + userId +
			", textQueryConsumerPath='" + textQueryConsumerPath + '\'' +
			", container=[" + (
			container.entrySet().stream()
				.map(e -> e.getKey() + "->" + e.getValue().toString())
				.collect(Collectors.joining())
		) + ']' +
			'}';
	}
}
