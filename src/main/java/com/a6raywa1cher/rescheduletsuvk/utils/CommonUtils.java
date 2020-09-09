package com.a6raywa1cher.rescheduletsuvk.utils;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.StringsConfigProperties;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CommonUtils {
	private StringsConfigProperties properties;

	@Autowired
	public CommonUtils(StringsConfigProperties properties) {
		this.properties = properties;
	}

	public String emojifyDigit(int digit) {
		return 0 <= digit && digit <= 9 ? properties.getDigits()[digit] : Integer.toString(digit);
	}

	public String convertLessonCells(DayOfWeek dayOfWeek, WeekSign weekSign, boolean today,
									 Collection<LessonCellMirror> lessonCellMirrors, boolean detailed) {
		return convertLessonCells(dayOfWeek, weekSign, today, lessonCellMirrors, detailed,
			true, false, true);
	}

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

	public String reduceLessonCells(Collection<LessonCellMirror> mirrors, boolean today, boolean detailed,
									boolean showTeachers, boolean showGroups, boolean showEmoji) {
		Set<String> subjectNames = new LinkedHashSet<>();
		Set<Pair<String, Integer>> groupsAndSubgroups = new LinkedHashSet<>();
		Set<Pair<String, String>> teachers = new LinkedHashSet<>();
		LocalTime start = null;
		LocalTime end = null;
		Integer columnPosition = null;
		boolean crossPair = false;
		boolean userMade = false;
		Multimap<String, List<String>> auditoriesAndAttributes = ArrayListMultimap.create();
		for (LessonCellMirror mirror : mirrors) {
			subjectNames.add(mirror.getFullSubjectName());
			if (mirror.getAuditoryAddress() != null)
				auditoriesAndAttributes.put(mirror.getAuditoryAddress(), mirror.getAttributes());
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
			userMade |= mirror.getUserMade() != null && mirror.getUserMade();
		}
		List<String> auditories = new ArrayList<>(auditoriesAndAttributes.keySet());
		auditories.sort(Comparator.naturalOrder());
		List<List<String>> attributes = auditories.stream()
			.sequential()
			.map(auditoriesAndAttributes::get)
			.map(c -> c.stream()
				.flatMap(Collection::stream)
				.distinct()
				.sorted()
				.collect(Collectors.toList())
			)
			.collect(Collectors.toList());
		LessonCellView lessonCellView = new LessonCellView(new ArrayList<>(subjectNames), new ArrayList<>(teachers),
			new ArrayList<>(groupsAndSubgroups), auditories, columnPosition, crossPair, start, end, userMade,
			attributes);
		return convertLessonView(lessonCellView, today, detailed, showTeachers, showGroups, showEmoji);
	}

	public String convertLessonCell(LessonCellMirror mirror, boolean today, boolean detailed) {
		return convertLessonCell(mirror, today, detailed, true, false, true);
	}

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
			mirror.getEnd(),
			mirror.getUserMade() != null && mirror.getUserMade(),
			Collections.singletonList(mirror.getAttributes())
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
			List<String> auditories = view.getAuditories();
			List<List<String>> allAttributes = view.getAttributes();
			String tail = IntStream.range(0, auditories.size())
				.mapToObj(i -> Pair.of(auditories.get(i), allAttributes.get(i)))
				.map(p -> {
					String auditoryAddress = p.getFirst();
					List<String> attributes = p.getSecond();
					String building = auditoryAddress.split("\\|")[0];
					String auditory = auditoryAddress.split("\\|")[1];
					return String.format("ауд.%s, к.%s", auditory, building) +
						(attributes.contains("ДОТ") ? " " + properties.getRemoteEmoji() : "");
				})
				.collect(Collectors.joining("; "));
			out.append(tail);
		}
		if (showEmoji) {
			out.append(subgroup ? " " + properties.getSingleSubgroupEmoji() : "") // is subgroup separated from another
				.append(view.isCrossPair() ? " " + properties.getCrossPairEmoji() : "") // is cross-pair
				.append(view.isUserMade() ? " " + properties.getUserMadeEmoji() : "");
		}
		if (detailed && showTeachers && view.getTeachersNames().size() != 0) {
			out.append('\n').append(properties.getTeacherEmoji()).append(' ');
			out.append(view.getTeachersNames().stream()
				.map(Pair::getFirst)
				.collect(Collectors.joining("; ")));
		}
		if (detailed && view.getAttributes().stream().flatMap(Collection::stream).anyMatch(s -> !s.equals("ДОТ"))) {
			out.append('\n').append(properties.getAttributesEmoji()).append(' ');
			List<String> strings = view.getAttributes().stream()
				.map(l -> String.join(", ", l))
				.collect(Collectors.toList());
			out.append(StringUtils.capitalize(String.join("; ", strings)));
		}
		if (showGroups) {
			out.append('\n').append(properties.getGroupsEmoji()).append(' ');
			out.append(view.getGroupsAndSubgroups().stream()
				.map(p -> p.getFirst() + (p.getSecond() != 0 ? ",п." + p.getSecond() : ""))
				.collect(Collectors.joining(", ")));
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

		private boolean userMade;

		private List<List<String>> attributes;
	}
}
