package com.a6raywa1cher.rescheduletsuvk.services.impls;

import com.a6raywa1cher.rescheduletsuvk.component.rts.NotFoundException;
import com.a6raywa1cher.rescheduletsuvk.component.rts.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetScheduleForWeekResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.submodels.DifferenceBetweenSubgroups;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {
	private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);
	private RtsServerRestComponent restComponent;
	private CommonUtils commonUtils;

	@Autowired
	public ScheduleServiceImpl(RtsServerRestComponent restComponent, CommonUtils commonUtils) {
		this.restComponent = restComponent;
		this.commonUtils = commonUtils;
	}

	@Override
	public CompletionStage<Optional<String>> getScheduleFor(String faculty, String group, Integer subgroup, LocalDate date, boolean today) {
		return restComponent.getRawSchedule(faculty, group)
				.thenCombine(restComponent.getWeekSign(faculty, date), (list, weekSignResult) -> {
					WeekSign weekSign = weekSignResult.getWeekSign();
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
						return Optional.of(commonUtils.convertLessonCells(date.getDayOfWeek(),
								weekSign, today, todayLessons, true));
					}
				})
				.exceptionally(e -> {
					log.error("Get lessons for " + faculty + ", "
							+ group + ", "
							+ subgroup + ", "
							+ date.toString() + " error", e);
					Sentry.capture(e);
					if (e.getCause() != null && e.getCause() instanceof NotFoundException) {
						throw (NotFoundException) e.getCause();
					}
					return Optional.empty();
				});
	}

	@Override
	public CompletionStage<Optional<String>> getNextLesson(String faculty, String group, Integer subgroup, LocalDateTime localDateTime) {
		return restComponent.getRawSchedule(faculty, group)
				.thenCombine(restComponent.getWeekSign(faculty), (list, weekSignResult) -> {
					Optional<LessonCellMirror> nextLesson = list.stream()
							.filter(cell -> cell.getDayOfWeek().equals(localDateTime.getDayOfWeek()))
							.filter(cell -> cell.getWeekSign().equals(WeekSign.ANY) ||
									cell.getWeekSign().equals(weekSignResult.getWeekSign()))
							.filter(cell -> cell.getStart().isAfter(localDateTime.toLocalTime()))
							.filter(cell -> cell.getSubgroup() == 0 || cell.getSubgroup().equals(subgroup))
							.findFirst();
					if (nextLesson.isEmpty()) {
						return Optional.<String>empty();
					} else {
						return Optional.of(commonUtils.convertLessonCell(nextLesson.get(), true, true));
					}
				})
				.exceptionally(e -> {
					log.error("Get next lesson for " + faculty + ", "
							+ group + ", "
							+ subgroup + ", "
							+ localDateTime.toString() + " error", e);
					Sentry.capture(e);
					if (e.getCause() != null && e.getCause() instanceof NotFoundException) {
						throw (NotFoundException) e.getCause();
					}
					return Optional.empty();
				});
	}

	@Override
	public CompletionStage<String> getScheduleForSevenDays(String faculty, String group, Integer subgroup, LocalDate date) {
		return restComponent.getScheduleForWeek(faculty, group, date)
				.thenApply(response -> {
					StringBuilder sb = new StringBuilder();
					boolean today = response.getSchedules().get(0).getDayOfWeek() == LocalDate.now().getDayOfWeek();
					for (GetScheduleForWeekResponse.Schedule schedule : response.getSchedules()) {
						sb.append(commonUtils.convertLessonCells(schedule.getDayOfWeek(), schedule.getSign(),
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
					if (e.getCause() != null && e.getCause() instanceof NotFoundException) {
						throw (NotFoundException) e.getCause();
					}
					return null;
				});
	}

	@Override
	public CompletionStage<List<LessonCellMirror>> getRawSchedule(String faculty, String group) {
		return restComponent.getRawSchedule(faculty, group)
				.exceptionally(e -> {
					log.error("Get raw schedule for " + faculty + ", "
							+ group + ", error", e);
					Sentry.capture(e);
					if (e.getCause() != null && e.getCause() instanceof NotFoundException) {
						throw (NotFoundException) e.getCause();
					}
					return new ArrayList<>();
				});
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
