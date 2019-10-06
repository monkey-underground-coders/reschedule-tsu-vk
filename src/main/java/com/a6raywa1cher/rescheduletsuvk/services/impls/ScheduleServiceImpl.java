package com.a6raywa1cher.rescheduletsuvk.services.impls;

import com.a6raywa1cher.rescheduletsuvk.component.backend.BackendComponent;
import com.a6raywa1cher.rescheduletsuvk.component.lessoncellrenderer.LessonCellRenderer;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetTeacherRawScheduleResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.Schedule;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.FindTeacherStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.StudentUser;
import com.a6raywa1cher.rescheduletsuvk.models.TeacherUser;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.submodels.DifferenceBetweenSubgroups;
import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {
	private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);
	private BackendComponent restComponent;
	private AppConfigProperties properties;
	private FindTeacherStageStringsConfigProperties findTeacherStageStringsConfigProperties;
	private LessonCellRenderer lessonCellRenderer;

	@Autowired
	public ScheduleServiceImpl(BackendComponent restComponent, AppConfigProperties properties,
	                           FindTeacherStageStringsConfigProperties findTeacherStageStringsConfigProperties,
	                           LessonCellRenderer lessonCellRenderer) {
		this.restComponent = restComponent;
		this.properties = properties;
		this.findTeacherStageStringsConfigProperties = findTeacherStageStringsConfigProperties;
		this.lessonCellRenderer = lessonCellRenderer;
	}

	private Optional<String> $getScheduleFor(List<LessonCellMirror> list, WeekSign weekSign, LocalDate date, Integer subgroup, boolean today) {
		List<LessonCellMirror> todayLessons;
		if (subgroup != null) {
			todayLessons = list.stream()
					.filter(cell -> cell.getDayOfWeek().equals(date.getDayOfWeek()))
					.filter(cell -> cell.getWeekSign().equals(WeekSign.ANY) ||
							cell.getWeekSign().equals(weekSign))
					.filter(cell -> cell.getSubgroup() == 0 || cell.getSubgroup().equals(subgroup))
					.collect(Collectors.toList());
		} else {
			todayLessons = list.stream()
					.filter(cell -> cell.getDayOfWeek().equals(date.getDayOfWeek()))
					.filter(cell -> cell.getWeekSign().equals(WeekSign.ANY) ||
							cell.getWeekSign().equals(weekSign))
					.collect(Collectors.toList());
		}
		if (todayLessons.isEmpty()) {
			return Optional.<String>empty();
		} else {
			return Optional.of(lessonCellRenderer.convertLessonCells(date.getDayOfWeek(),
					weekSign, today, todayLessons, true));
		}
	}

	@Override
	public CompletionStage<Optional<String>> getScheduleFor(String faculty, String group, Integer subgroup, LocalDate date, boolean today) {
		return restComponent.getRawSchedule(faculty, group)
				.thenCombine(restComponent.getWeekSign(faculty, date), (list, weekSignResult) -> {
					WeekSign weekSign = weekSignResult.getWeekSign();
					return $getScheduleFor(list, weekSign, date, subgroup, today);
				})
				.exceptionally(e -> {
					log.error("Get lessons for " + faculty + ", "
							+ group + ", "
							+ subgroup + ", "
							+ date.toString() + " error", e);
					Sentry.capture(e);
					return Optional.empty();
				});
	}

	@Override
	public CompletionStage<Optional<String>> getScheduleFor(String teacherName, LocalDate date, boolean today) {
		return restComponent.getTeacherRawSchedule(teacherName)
				.thenCombine(restComponent.getWeekSign(properties.getPrimaryFaculty(), date), (o, weekSignResult) -> {
					WeekSign weekSign = weekSignResult.getWeekSign();
					return $getScheduleFor(o.getRawSchedule(), weekSign, date, null, today);
				})
				.exceptionally(e -> {
					log.error("Get lessons for " + teacherName + ", "
							+ date.toString() + " error", e);
					Sentry.capture(e);
					return Optional.empty();
				});
	}

	@Override
	public CompletionStage<Optional<String>> getScheduleFor(UserInfo userInfo, LocalDate date, boolean today) {
		if (userInfo instanceof StudentUser) {
			StudentUser studentUser = (StudentUser) userInfo;
			return getScheduleFor(studentUser.getFacultyId(), studentUser.getGroupId(),
					studentUser.getSubgroup(), date, today);
		} else if (userInfo instanceof TeacherUser) {
			TeacherUser teacherName = (TeacherUser) userInfo;
			return getScheduleFor(teacherName.getTeacherName(), date, today);
		} else if (userInfo != null) {
			throw new IllegalArgumentException("Unknown userInfo impl: " + userInfo.getClass().getName());
		}
		return CompletableFuture.completedStage(null);
	}

	private Optional<String> $getNextLesson(List<LessonCellMirror> list, WeekSign weekSign, LocalDateTime localDateTime, Integer subgroup) {
		Optional<LessonCellMirror> nextLesson = list.stream()
				.filter(cell -> cell.getDayOfWeek().equals(localDateTime.getDayOfWeek()))
				.filter(cell -> cell.getWeekSign().equals(WeekSign.ANY) ||
						cell.getWeekSign().equals(weekSign))
				.filter(cell -> cell.getStart().isAfter(localDateTime.toLocalTime()))
				.filter(cell -> cell.getSubgroup() == 0 || cell.getSubgroup().equals(subgroup) || subgroup == null)
				.findFirst();
		if (nextLesson.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(lessonCellRenderer.convertLessonCell(nextLesson.get(), true, true));
		}
	}

	@Override
	public CompletionStage<Optional<String>> getNextLesson(String faculty, String group, Integer subgroup, LocalDateTime localDateTime) {
		return restComponent.getRawSchedule(faculty, group)
				.thenCombine(restComponent.getWeekSign(faculty), (list, weekSignResult) ->
						$getNextLesson(list, weekSignResult.getWeekSign(), localDateTime, subgroup))
				.exceptionally(e -> {
					log.error("Get next lesson for " + faculty + ", "
							+ group + ", "
							+ subgroup + ", "
							+ localDateTime.toString() + " error", e);
					Sentry.capture(e);
					return Optional.empty();
				});
	}

	@Override
	public CompletionStage<Optional<String>> getNextLesson(String teacherName, LocalDateTime date) {
		return restComponent.getTeacherRawSchedule(teacherName)
				.thenCombine(restComponent.getWeekSign(properties.getPrimaryFaculty()), (list, weekSignResult) ->
						$getNextLesson(list.getRawSchedule(), weekSignResult.getWeekSign(), date, null))
				.exceptionally(e -> {
					log.error("Get next lesson for " + teacherName + ", "
							+ date.toString() + " error", e);
					Sentry.capture(e);
					return Optional.empty();
				});
	}

	@Override
	public CompletionStage<Optional<String>> getNextLesson(UserInfo userInfo, LocalDateTime date) {
		if (userInfo instanceof StudentUser) {
			StudentUser studentUser = (StudentUser) userInfo;
			return getNextLesson(studentUser.getFacultyId(), studentUser.getGroupId(),
					studentUser.getSubgroup(), date);
		} else if (userInfo instanceof TeacherUser) {
			TeacherUser teacherName = (TeacherUser) userInfo;
			return getNextLesson(teacherName.getTeacherName(), date);
		} else if (userInfo != null) {
			throw new IllegalArgumentException("Unknown userInfo impl: " + userInfo.getClass().getName());
		}
		return CompletableFuture.completedStage(null);
	}

	@Override
	public CompletionStage<String> getScheduleForSevenDays(String faculty, String group, Integer subgroup, LocalDate date) {
		return restComponent.getScheduleForWeek(faculty, group, date)
				.thenApply(response -> {
					List<Schedule> schedules = response.getSchedules();
					StringBuilder sb = new StringBuilder();
					boolean today = schedules.get(0).getDayOfWeek() == LocalDate.now().getDayOfWeek();
					for (Schedule schedule : schedules) {
						sb.append(lessonCellRenderer.convertLessonCells(schedule.getDayOfWeek(), schedule.getWeekSign(),
								today, schedule.getCells().stream()
										.filter(cell -> cell.getSubgroup() == 0 || cell.getSubgroup().equals(subgroup))
										.collect(Collectors.toList()), false));
						today = false;
					}
					return sb.toString();
				})
				.exceptionally(e -> {
					log.error("Get seven days error for " + faculty + ", "
							+ group + ", "
							+ subgroup + ", "
							+ date.toString() + " error", e);
					Sentry.capture(e);
					return null;
				});
	}

	@Override
	public CompletionStage<String> getScheduleForSevenDays(String teacherName, LocalDate date, boolean includeHeader) {
		return restComponent.getTeacherWeekSchedule(teacherName)
				.thenApply(response -> {
					Optional<LessonCellMirror> anyCell = response.getSchedules().stream()
							.flatMap(schedule -> schedule.getCells().stream()).findAny();
					if (anyCell.isEmpty()) {
						return "";
					}
					StringBuilder sb = new StringBuilder(!includeHeader ? "" :
							MessageFormat.format(findTeacherStageStringsConfigProperties.getResultHeader(), teacherName,
									StringUtils.defaultIfBlank(anyCell.get().getTeacherTitle(),
											findTeacherStageStringsConfigProperties.getTeacherTitlePlaceholder()
									)
							) + '\n');
					boolean today = response.getSchedules().get(0).getDayOfWeek() == LocalDate.now().getDayOfWeek();
					for (Schedule schedule : response.getSchedules()) {
						sb.append(lessonCellRenderer.convertLessonCells(schedule.getDayOfWeek(), schedule.getWeekSign(),
								today, schedule.getCells(), false, false, true, true));
						today = false;
					}
					return sb.toString();
				})
				.exceptionally(e -> {
					log.error("Get next lesson for " + teacherName + ", "
							+ date.toString() + " error", e);
					Sentry.capture(e);
					return null;
				});
	}

	@Override
	public CompletionStage<String> getScheduleForSevenDays(UserInfo userInfo, LocalDate date, boolean includeHeader) {
		if (userInfo instanceof StudentUser) {
			StudentUser studentUser = (StudentUser) userInfo;
			return getScheduleForSevenDays(studentUser.getFacultyId(), studentUser.getGroupId(),
					studentUser.getSubgroup(), date);
		} else if (userInfo instanceof TeacherUser) {
			TeacherUser teacherName = (TeacherUser) userInfo;
			return getScheduleForSevenDays(teacherName.getTeacherName(), date, includeHeader);
		} else if (userInfo != null) {
			throw new IllegalArgumentException("Unknown userInfo impl: " + userInfo.getClass().getName());
		}
		return CompletableFuture.completedStage(null);
	}

	@Override
	public CompletionStage<List<LessonCellMirror>> getRawSchedule(String faculty, String group) {
		return restComponent.getRawSchedule(faculty, group)
				.exceptionally(e -> {
					log.error("Get raw schedule for " + faculty + ", "
							+ group + ", error", e);
					Sentry.capture(e);
					return new ArrayList<>();
				});
	}

	@Override
	public CompletionStage<List<LessonCellMirror>> getRawSchedule(String teacherName) {
		return restComponent.getTeacherRawSchedule(teacherName)
				.thenApply(GetTeacherRawScheduleResponse::getRawSchedule)
				.exceptionally(e -> {
					log.error("Get raw schedule for " + teacherName + " error", e);
					Sentry.capture(e);
					return new ArrayList<>();
				});
	}

	@Override
	public CompletionStage<List<LessonCellMirror>> getRawSchedule(UserInfo userInfo) {
		if (userInfo instanceof StudentUser) {
			StudentUser studentUser = (StudentUser) userInfo;
			return getRawSchedule(studentUser.getFacultyId(), studentUser.getGroupId());
		} else if (userInfo instanceof TeacherUser) {
			TeacherUser teacherName = (TeacherUser) userInfo;
			return getRawSchedule(teacherName.getTeacherName());
		} else if (userInfo != null) {
			throw new IllegalArgumentException("Unknown userInfo impl: " + userInfo.getClass().getName());
		}
		return CompletableFuture.completedStage(null);
	}

	@Override
	public CompletionStage<DifferenceBetweenSubgroups> findDifferenceBetweenSubgroups(String faculty, String group) {
		return this.getRawSchedule(faculty, group)
				.thenApply(schedule -> {
					// find situation (no lesson) - (lesson)
					LessonCellMirror firstSituation = null;
					// find situation (lesson1) - (lesson2)
					Pair<LessonCellMirror, LessonCellMirror> secondSituation = null;
					for (LessonCellMirror mirror : schedule) {
						if (mirror.getSubgroup() == 0) continue;
						boolean found = false;
						for (LessonCellMirror second : schedule) {
							if (second.getSubgroup() == 0) continue;
							if (mirror.getWeekSign() == second.getWeekSign() &&
									mirror.getDayOfWeek().equals(second.getDayOfWeek()) &&
									mirror.getColumnPosition().equals(second.getColumnPosition()) &&
									!mirror.getSubgroup().equals(second.getSubgroup())
							) {
								if (secondSituation == null) {
									secondSituation = Pair.of(mirror, second);
								}
								found = true;
								break;
							}
						}
						if (!found) {
							firstSituation = mirror;
							break;
						}
					}
					if (firstSituation != null) {
						return new DifferenceBetweenSubgroups(firstSituation, firstSituation.getSubgroup(), null,
								1 + (firstSituation.getSubgroup() % 2));
					} else if (secondSituation != null) {
						return new DifferenceBetweenSubgroups(secondSituation.getFirst(), secondSituation.getFirst().getSubgroup(),
								secondSituation.getSecond(), secondSituation.getSecond().getSubgroup());
					} else {
						throw new RuntimeException("WTF? " + faculty + ", " + group);
					}
				})
				.exceptionally(e -> {
					log.error("Find difference between subgroup for " + faculty + ", "
							+ group + " error", e);
					Sentry.capture(e);
					return null;
				});
	}
}
