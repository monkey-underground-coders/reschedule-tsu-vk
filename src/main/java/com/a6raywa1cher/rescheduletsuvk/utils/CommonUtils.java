package com.a6raywa1cher.rescheduletsuvk.utils;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Collection;
import java.util.Locale;

public class CommonUtils {
	public static final String CROSS_PAIR_EMOJI = "\ud83d\udd17";
	public static final String SINGLE_SUBGROUP_EMOJI = "\ud83d\udc64";
	public static final String TEACHER_EMOJI = "\ud83d\udc68\u200d\ud83c\udfeb";
	public static final String PAST_LESSON_EMOJI = "\u26d4";
	public static final String LIVE_LESSON_EMOJI = "\u25b6\ufe0f";
	public static final String FUTURE_LESSON_EMOJI = "\u23f8\ufe0f";
	public static final String COOKIES_EMOJI = "\uD83C\uDF6A";
	public static final String ARROW_DOWN_EMOJI = "\u2b07\ufe0f";
	public static final String ARROW_RIGHT_EMOJI = "\u27a1\ufe0f";

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

	public static String convertLessonCells(DayOfWeek dayOfWeek, WeekSign weekSign, boolean today,
	                                        Collection<LessonCellMirror> lessonCellMirrors, boolean detailed) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s (%s):\n",
				dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru-RU")),
				weekSign.getPrettyString()
		));
		for (LessonCellMirror cellMirror : lessonCellMirrors) {
			sb.append(CommonUtils.convertLessonCell(cellMirror, today, detailed)).append('\n');
		}
		sb.append('\n');
		return sb.toString();
	}

	private static String findFirstCapitalLetter(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (Character.isUpperCase(str.charAt(i))) {
				return Character.toString(str.charAt(i));
			}
		}
		return Character.toString(Character.toUpperCase(str.charAt(0)));
	}

	public static String convertLessonCell(LessonCellMirror mirror, boolean today, boolean detailed) {
		String building = mirror.getAuditoryAddress().split("\\|")[0];
		String auditory = mirror.getAuditoryAddress().split("\\|")[1];
		boolean subgroup = mirror.getSubgroup() != 0;
		String out = emojifyDigit(mirror.getColumnPosition() + 1);
		if (today) {
			LocalTime localTime = LocalTime.now();
			if (mirror.getStart().isBefore(localTime) && mirror.getEnd().isAfter(localTime)) { // live lesson
				out += LIVE_LESSON_EMOJI;
			} else if (mirror.getStart().isAfter(localTime)) { // not yet live
				out += FUTURE_LESSON_EMOJI;
			} else { // already passed
				out += PAST_LESSON_EMOJI;
			}
		}
		out += String.format(" (%s - %s) %s, ",
				mirror.getStart().format(DateTimeFormatter.ofPattern("HH:mm")),
				mirror.getEnd().format(DateTimeFormatter.ofPattern("HH:mm")),
				mirror.getFullSubjectName());
		if (!detailed) {
			String[] fullTeacherName = mirror.getTeacherName().split(" ");
			for (int i = 1; i < fullTeacherName.length; i++) {
				fullTeacherName[i] = findFirstCapitalLetter(fullTeacherName[i]);
			}
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < fullTeacherName.length; i++) {
				sb.append(fullTeacherName[i]);
				if (i != 0) {
					sb.append('.');
				} else {
					sb.append(' ');
				}
			}
			out += String.format("%s, ", sb.toString());
		}
		out += String.format("ауд.%s, к.%s",
				auditory, building);
		out += (subgroup ? " " + SINGLE_SUBGROUP_EMOJI : "") // is subgroup separated from another subgroup
				+ (mirror.getCrossPair() ? " " + CROSS_PAIR_EMOJI : ""); // is cross-pair
		if (detailed) {
			out += String.format("\n" + TEACHER_EMOJI + " %s %s\n",
					mirror.getTeacherTitle(), mirror.getTeacherName());
		}
		return out;
	}
}
