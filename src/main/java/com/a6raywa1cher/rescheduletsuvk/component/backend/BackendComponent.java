package com.a6raywa1cher.rescheduletsuvk.component.backend;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.*;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface BackendComponent {
	CompletionStage<GetFacultiesResponse> getFaculties();

	CompletionStage<GetScheduleForWeekResponse> getScheduleForWeek(String facultyId, String groupId);

	CompletionStage<GetScheduleForWeekResponse> getScheduleForWeek(String facultyId, String groupId, LocalDate date);

	CompletionStage<GetGroupsResponse> getGroups(String facultyId);

	CompletionStage<List<LessonCellMirror>> getRawSchedule(String facultyId, String groupId);

	CompletionStage<GetWeekSignResponse> getWeekSign(String facultyId);

	CompletionStage<GetWeekSignResponse> getWeekSign(String facultyId, LocalDate localDate);

	CompletionStage<GetTeachersResponse> findTeacher(String teacherName);

	CompletionStage<GetScheduleOfTeacherForWeekResponse> getTeacherWeekSchedule(String teacherName);

	CompletionStage<GetTeacherRawScheduleResponse> getTeacherRawSchedule(String teacherName);
}
