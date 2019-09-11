package com.a6raywa1cher.rescheduletsuvk.utils;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CommonUtils {
	public static String emojifyDigit(int digit) {
		String[] digits = new String[]{
				/* 0 */ "\u0030\ufe0f\u20e3",
				/* 1 */ "\u0031\ufe0f\u20e3",
				/* 2 */ "\u0032\ufe0f\u20e3",
				/* 3 */ "\u0033\ufe0f\u20e3",
				/* 4 */ "\u0034\ufe0f\u20e3",
				/* 5 */ "\u0035\ufe0f\u20e3",
				/* 6 */ "\u0036\ufe0f\u20e3",
				/* 7 */ "\u0037\ufe0f\u20e3",
				/* 8 */ "\u0038\ufe0f\u20e3",
				/* 9 */ "\u0039\ufe0f\u20e3"
		};
		return 0 <= digit && digit <= 9 ? digits[digit] : Integer.toString(digit);
	}

	public static String convertLessonCell(LessonCellMirror mirror, boolean today) {
		String building = mirror.getAuditoryAddress().split("\\|")[0];
		String auditory = mirror.getAuditoryAddress().split("\\|")[1];
		if (today) {
			LocalTime localTime = LocalTime.now();
			if (mirror.getStart().isBefore(localTime) && mirror.getEnd().isAfter(localTime)) { // live pair
				return String.format("%s\u25b6\ufe0f (%s - %s) %s, ауд.%s, к.%s",
						emojifyDigit(mirror.getColumnPosition() + 1),
						mirror.getStart().format(DateTimeFormatter.ofPattern("HH:mm")),
						mirror.getEnd().format(DateTimeFormatter.ofPattern("HH:mm")),
						mirror.getFullSubjectName(),
						auditory, building);
			} else if (mirror.getStart().isAfter(localTime)) { // not yet live
				return String.format("%s\u23f8\ufe0f (%s - %s) %s, ауд.%s, к.%s",
						emojifyDigit(mirror.getColumnPosition() + 1),
						mirror.getStart().format(DateTimeFormatter.ofPattern("HH:mm")),
						mirror.getEnd().format(DateTimeFormatter.ofPattern("HH:mm")),
						mirror.getFullSubjectName(),
						auditory, building);
			} else { // already passed
				return String.format("%s\u23ea (%s - %s) %s, ауд.%s, к.%s",
						emojifyDigit(mirror.getColumnPosition() + 1),
						mirror.getStart().format(DateTimeFormatter.ofPattern("HH:mm")),
						mirror.getEnd().format(DateTimeFormatter.ofPattern("HH:mm")),
						mirror.getFullSubjectName(),
						auditory, building);
			}
		} else {
			return String.format("%s (%s - %s) %s, ауд.%s, к.%s",
					emojifyDigit(mirror.getColumnPosition() + 1),
					mirror.getStart().format(DateTimeFormatter.ofPattern("HH:mm")),
					mirror.getEnd().format(DateTimeFormatter.ofPattern("HH:mm")),
					mirror.getFullSubjectName(),
					auditory, building);
		}
	}
}
