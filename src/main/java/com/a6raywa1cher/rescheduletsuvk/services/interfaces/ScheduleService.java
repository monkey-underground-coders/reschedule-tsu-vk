package com.a6raywa1cher.rescheduletsuvk.services.interfaces;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.submodels.DifferenceBetweenSubgroups;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface ScheduleService {
	CompletionStage<Optional<String>> getScheduleFor(String faculty, String group, Integer subgroup, LocalDate date, boolean today);

	CompletionStage<Optional<String>> getScheduleFor(String teacherName, LocalDate date, boolean today);

	CompletionStage<Optional<String>> getScheduleFor(UserInfo userInfo, LocalDate date, boolean today);

	CompletionStage<Optional<String>> getNextLesson(String faculty, String group, Integer subgroup, LocalDateTime date);

	CompletionStage<Optional<String>> getNextLesson(String teacherName, LocalDateTime date);

	CompletionStage<Optional<String>> getNextLesson(UserInfo userInfo, LocalDateTime date);

	CompletionStage<String> getScheduleForSevenDays(String faculty, String group, Integer subgroup, LocalDate date);

	CompletionStage<String> getScheduleForSevenDays(String teacherName, LocalDate date, boolean includeHeader);

	CompletionStage<String> getScheduleForSevenDays(UserInfo userInfo, LocalDate date, boolean includeHeader);

	CompletionStage<List<LessonCellMirror>> getRawSchedule(String faculty, String group);

	CompletionStage<List<LessonCellMirror>> getRawSchedule(String teacherName);

	CompletionStage<List<LessonCellMirror>> getRawSchedule(UserInfo userInfo);

	CompletionStage<DifferenceBetweenSubgroups> findDifferenceBetweenSubgroups(String faculty, String group);
}
