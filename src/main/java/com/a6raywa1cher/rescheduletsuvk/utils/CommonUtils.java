package com.a6raywa1cher.rescheduletsuvk.utils;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.util.Pair;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

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
	public static final String GROUPS_EMOJI = "\uD83D\uDC65";

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
		return convertLessonCells(dayOfWeek, weekSign, today, lessonCellMirrors, detailed,
				true, false);
	}

	public static String convertLessonCells(DayOfWeek dayOfWeek, WeekSign weekSign, boolean today,
	                                        Collection<LessonCellMirror> lessonCellMirrors, boolean detailed,
	                                        boolean showTeachers, boolean showGroups) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s (%s):\n",
				dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru-RU")),
				weekSign.getPrettyString()
		));
		Map<Integer, Set<LessonCellMirror>> map = new HashMap<>();
		for (LessonCellMirror cellMirror : lessonCellMirrors) {
			if (!map.containsKey(cellMirror.getColumnPosition())) {
				map.put(cellMirror.getColumnPosition(), new HashSet<>());
			}
			map.get(cellMirror.getColumnPosition()).add(cellMirror);
		}
		List<Integer> order = new ArrayList<>(map.keySet());
		order.sort(Comparator.naturalOrder());
		for (Set<LessonCellMirror> cellMirror : order.stream().map(map::get).collect(Collectors.toList())) {
			if (cellMirror.size() == 1) {
				sb.append(CommonUtils.convertLessonCell(cellMirror.iterator().next(), today, detailed, showTeachers,
						showGroups)).append('\n');
			} else {
				sb.append(CommonUtils.reduceLessonCells(cellMirror, today, detailed, showTeachers, showGroups))
						.append('\n');
			}
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

	public static String reduceLessonCells(Collection<LessonCellMirror> mirrors, boolean today, boolean detailed,
	                                       boolean showTeachers, boolean showGroups) {
		Set<String> subjectNames = new LinkedHashSet<>();
		Set<String> auditories = new LinkedHashSet<>();
		Set<Pair<String, Integer>> groupsAndSubgroups = new LinkedHashSet<>();
		Set<Pair<String, String>> teachers = new LinkedHashSet<>();
		LocalTime start = null;
		LocalTime end = null;
		Integer columnPosition = null;
		boolean crossPair = false;
		for (LessonCellMirror mirror : mirrors) {
			subjectNames.add(mirror.getFullSubjectName());
			auditories.add(mirror.getAuditoryAddress());
			groupsAndSubgroups.add(Pair.of(mirror.getGroup(), mirror.getSubgroup()));
			teachers.add(Pair.of(mirror.getTeacherName(), mirror.getTeacherTitle()));
			if (start == null) {
				start = mirror.getStart();
			} else if (start.isAfter(mirror.getStart())) {
				start = mirror.getStart();
			}
			if (end == null) {
				end = mirror.getEnd();
			} else if (end.isBefore(mirror.getEnd())) {
				end = mirror.getEnd();
			}
			if (columnPosition == null) {
				columnPosition = mirror.getColumnPosition();
			} else if (!columnPosition.equals(mirror.getColumnPosition())) {
				throw new IllegalArgumentException("Provided cells with different positions: "
						+ columnPosition + " and " + mirror.getColumnPosition());
			}
			crossPair = crossPair | mirror.getCrossPair();
		}
		LessonCellView lessonCellView = new LessonCellView(new ArrayList<>(subjectNames), new ArrayList<>(teachers),
				new ArrayList<>(groupsAndSubgroups), new ArrayList<>(auditories), columnPosition,
				crossPair, start, end);
		return convertLessonView(lessonCellView, today, detailed, showTeachers, showGroups);
	}

	public static String convertLessonCell(LessonCellMirror mirror, boolean today, boolean detailed) {
		return convertLessonCell(mirror, today, detailed, true, false);
	}

	public static String convertLessonCell(LessonCellMirror mirror, boolean today, boolean detailed,
	                                       boolean showTeachers, boolean showGroups) {
		LessonCellView lessonCellView = new LessonCellView(
				Collections.singletonList(mirror.getFullSubjectName()),
				Collections.singletonList(Pair.of(mirror.getTeacherName(), mirror.getTeacherTitle())),
				Collections.singletonList(Pair.of(mirror.getGroup(), mirror.getSubgroup())),
				Collections.singletonList(mirror.getAuditoryAddress()),
				mirror.getColumnPosition(), mirror.getCrossPair(), mirror.getStart(),
				mirror.getEnd()
		);
		return convertLessonView(lessonCellView, today, detailed, showTeachers, showGroups);
	}

	private static String convertLessonView(LessonCellView view, boolean today, boolean detailed,
	                                        boolean showTeachers, boolean showGroups) {
		boolean subgroup = view.getGroupsAndSubgroups().size() == 1 &&
				view.getGroupsAndSubgroups().get(0).getSecond() != 0;
		StringBuilder out = new StringBuilder(emojifyDigit(view.getColumnPosition() + 1));
		if (today) {
			LocalTime localTime = LocalTime.now();
			if (view.getStart().isBefore(localTime) && view.getEnd().isAfter(localTime)) { // live lesson
				out.append(LIVE_LESSON_EMOJI);
			} else if (view.getStart().isAfter(localTime)) { // not yet live
				out.append(FUTURE_LESSON_EMOJI);
			} else { // already passed
				out.append(PAST_LESSON_EMOJI);
			}
		}
		out.append(String.format(" (%s - %s) ",
				view.getStart().format(DateTimeFormatter.ofPattern("HH:mm")),
				view.getEnd().format(DateTimeFormatter.ofPattern("HH:mm"))));
		out.append(view.getSubjectNames().stream().map(name -> name + ", ").collect(Collectors.joining()));
		if (!detailed && showTeachers && view.getTeachersNames().size() != 0) {
			for (Pair<String, String> pair : view.getTeachersNames()) {
				String teacherName = pair.getFirst();
				String[] fullTeacherName = teacherName.split(" ");
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
				out.append(String.format("%s, ", sb.toString()));
			}
		}
		if (view.getAuditories().size() != 0) {
			for (String auditoryAddress : view.getAuditories()) {
				String building = auditoryAddress.split("\\|")[0];
				String auditory = auditoryAddress.split("\\|")[1];
				out.append(String.format("ауд.%s, к.%s ",
						auditory, building));
			}
		}
		out.append(subgroup ? " " + SINGLE_SUBGROUP_EMOJI : "") // is subgroup separated from another
				.append(view.isCrossPair() ? " " + CROSS_PAIR_EMOJI : ""); // is cross-pair
		if (detailed && showTeachers && view.getTeachersNames().size() != 0) {
//			out.append(String.format("\n" + TEACHER_EMOJI + " %s %s\n",
//					mirror.getTeacherTitle(), mirror.getTeacherName()));
			out.append('\n').append(TEACHER_EMOJI).append(' ');
			for (Pair<String, String> pair : view.getTeachersNames()) {
				out.append(pair.getSecond()).append(", ").append(pair.getFirst()).append("; ");
			}
		}
		if (showGroups) {
			out.append('\n').append(GROUPS_EMOJI).append(' ');
			boolean first = true;
			for (Pair<String, Integer> pair : view.getGroupsAndSubgroups()) {
				if (first) {
					first = false;
				} else {
					out.append(", ");
				}
				out.append(pair.getFirst());
				if (pair.getSecond() != 0) {
					out.append(",п.").append(pair.getSecond());
				}
			}
		}
		return out.toString();
	}

	@Data
	@AllArgsConstructor
	private static class LessonCellView {
		private List<String> subjectNames;

		// first - name, second - title
		private List<Pair<String, String>> teachersNames;

		private List<Pair<String, Integer>> groupsAndSubgroups;

		private List<String> auditories;

		private Integer columnPosition;

		private boolean crossPair;

		private LocalTime start;

		private LocalTime end;
	}
}
