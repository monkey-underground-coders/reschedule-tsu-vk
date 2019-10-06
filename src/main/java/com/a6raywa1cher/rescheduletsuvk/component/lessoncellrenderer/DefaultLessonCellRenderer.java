package com.a6raywa1cher.rescheduletsuvk.component.lessoncellrenderer;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.StringsConfigProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DefaultLessonCellRenderer implements LessonCellRenderer {
	private StringsConfigProperties properties;

	@Autowired
	public DefaultLessonCellRenderer(StringsConfigProperties properties) {
		this.properties = properties;
	}

	@Override
	public String emojifyDigit(int digit) {
		return 0 <= digit && digit <= 9 ? properties.getDigits()[digit] : Integer.toString(digit);
	}

	@Override
	public String reduceFullName(String fullName) {
		String[] split = fullName.split(" ");
		for (int i = 1; i < split.length; i++) {
			split[i] = findFirstCapitalLetter(split[i]);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < split.length; i++) {
			sb.append(split[i]);
			if (i != 0) {
				sb.append('.');
			} else {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	@Override
	public String convertLessonCells(DayOfWeek dayOfWeek, WeekSign weekSign, boolean today,
	                                 Collection<LessonCellMirror> lessonCellMirrors, boolean detailed) {
		return convertLessonCells(dayOfWeek, weekSign, today, lessonCellMirrors, detailed,
				true, false, true);
	}

	@Override
	public String convertLessonCells(DayOfWeek dayOfWeek, WeekSign weekSign, boolean today,
	                                 Collection<LessonCellMirror> lessonCellMirrors, boolean detailed,
	                                 boolean showTeachers, boolean showGroups, boolean showEmoji) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s (%s):\n",
				dayOfWeek.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru-RU")).toUpperCase(),
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
				sb.append(convertLessonCell(cellMirror.iterator().next(), today, detailed, showTeachers,
						showGroups, showEmoji)).append('\n');
			} else {
				sb.append(reduceLessonCells(cellMirror, today, detailed, showTeachers, showGroups, showEmoji))
						.append('\n');
			}
			sb.append('\n');
		}
		sb.append('\n');
		return sb.toString();
	}

	private String findFirstCapitalLetter(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (Character.isUpperCase(str.charAt(i))) {
				return Character.toString(str.charAt(i));
			}
		}
		return Character.toString(Character.toUpperCase(str.charAt(0)));
	}

	@Override
	public String reduceLessonCells(Collection<LessonCellMirror> mirrors, boolean today, boolean detailed,
	                                boolean showTeachers, boolean showGroups, boolean showEmoji) {
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
			if (mirror.getAuditoryAddress() != null) auditories.add(mirror.getAuditoryAddress());
			groupsAndSubgroups.add(Pair.of(mirror.getGroup(), mirror.getSubgroup()));
			if (mirror.getTeacherName() != null)
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
		return convertLessonView(lessonCellView, today, detailed, showTeachers, showGroups, showEmoji);
	}

	@Override
	public String convertLessonCell(LessonCellMirror mirror, boolean today, boolean detailed) {
		return convertLessonCell(mirror, today, detailed, true, false, true);
	}

	@Override
	public String convertLessonCell(LessonCellMirror mirror, boolean today, boolean detailed,
	                                boolean showTeachers, boolean showGroups, boolean showEmoji) {
		LessonCellView lessonCellView = new LessonCellView(
				Collections.singletonList(mirror.getFullSubjectName()),
				(mirror.getTeacherName() == null) ? Collections.emptyList() :
						Collections.singletonList(Pair.of(mirror.getTeacherName(),
								mirror.getTeacherTitle() == null ? "" : mirror.getTeacherTitle())),
				Collections.singletonList(Pair.of(mirror.getGroup(), mirror.getSubgroup())),
				(mirror.getAuditoryAddress() == null) ? Collections.emptyList() :
						Collections.singletonList(mirror.getAuditoryAddress()),
				mirror.getColumnPosition(), mirror.getCrossPair(), mirror.getStart(),
				mirror.getEnd()
		);
		return convertLessonView(lessonCellView, today, detailed, showTeachers, showGroups, showEmoji);
	}

	private String convertLessonView(LessonCellView view, boolean today, boolean detailed,
	                                 boolean showTeachers, boolean showGroups, boolean showEmoji) {
		boolean subgroup = view.getGroupsAndSubgroups().size() == 1 &&
				view.getGroupsAndSubgroups().get(0).getSecond() != 0;
		StringBuilder out = new StringBuilder(emojifyDigit(view.getColumnPosition() + 1));
		if (today) {
			LocalTime localTime = LocalTime.now();
			if (view.getStart().isBefore(localTime) && view.getEnd().isAfter(localTime)) { // live lesson
				out.append(properties.getLiveLessonEmoji());
			} else if (view.getStart().isAfter(localTime)) { // not yet live
				out.append(properties.getFutureLessonEmoji());
			} else { // already passed
				out.append(properties.getPastLessonEmoji());
			}
		}
		out.append(String.format(" %s - %s: ",
				view.getStart().format(DateTimeFormatter.ofPattern("HH:mm")),
				view.getEnd().format(DateTimeFormatter.ofPattern("HH:mm"))));
		out.append(view.getSubjectNames().stream().map(name -> name + ", ").collect(Collectors.joining()));
		if (!detailed && showTeachers && view.getTeachersNames().size() != 0) {
			for (Pair<String, String> pair : view.getTeachersNames()) {
				String teacherName = pair.getFirst();
				String reduced = reduceFullName(teacherName);
				out.append(String.format("%s, ", reduced));
			}
		}
		if (view.getAuditories().size() != 0) {
			boolean first = true;
			for (String auditoryAddress : view.getAuditories()) {
				if (first) {
					first = false;
				} else {
					out.append(", ");
				}
				String building = auditoryAddress.split("\\|")[0];
				String auditory = auditoryAddress.split("\\|")[1];
				out.append(String.format("ауд.%s, к.%s",
						auditory, building));
			}
		}
		if (showEmoji) {
			out.append(subgroup ? " " + properties.getSingleSubgroupEmoji() : "") // is subgroup separated from another
					.append(view.isCrossPair() ? " " + properties.getCrossPairEmoji() : ""); // is cross-pair
		}
		if (detailed && showTeachers && view.getTeachersNames().size() != 0) {
			out.append('\n').append(properties.getTeacherEmoji()).append(' ');
			boolean first = true;
			for (Pair<String, String> pair : view.getTeachersNames()) {
				if (first) {
					first = false;
				} else {
					out.append("; ");
				}
				out.append(pair.getFirst());
			}
		}
		if (showGroups) {
			out.append('\n').append(properties.getGroupsEmoji()).append(' ');
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