package com.a6raywa1cher.rescheduletsuvk.component.rtsmodels;

public enum WeekSign {
	ANY("="), PLUS("\u2795"), MINUS("\u2796");
	private final String prettyString;

	WeekSign(String prettyString) {
		this.prettyString = prettyString;
	}

	public static WeekSign inverse(WeekSign weekSign) {
		if (weekSign == ANY) return ANY;
		return weekSign == PLUS ? MINUS : PLUS;
	}

	public String getPrettyString() {
		return prettyString;
	}
}
