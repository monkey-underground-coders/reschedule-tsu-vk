package com.a6raywa1cher.rescheduletsuvk.utils;

import lombok.Data;

@Data
public class VkKeyboardButton {
	private Color color;
	private String label;
	private String payload;

	public VkKeyboardButton(Color color, String label, String payload) {
		this.color = color;
		this.label = label;
		this.payload = payload;
	}

	public VkKeyboardButton(Color color, String label) {
		this.color = color;
		this.label = label;
	}

	public enum Color {
		PRIMARY, SECONDARY, NEGATIVE, POSITIVE
	}
}
