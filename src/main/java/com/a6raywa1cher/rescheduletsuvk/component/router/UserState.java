package com.a6raywa1cher.rescheduletsuvk.component.router;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class UserState {
	private final int userId;

	private String textQueryConsumerPath;

	private Map<String, Object> container = new HashMap<>();
}
