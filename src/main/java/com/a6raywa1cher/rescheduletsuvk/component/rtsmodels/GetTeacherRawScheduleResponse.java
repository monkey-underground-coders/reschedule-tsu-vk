package com.a6raywa1cher.rescheduletsuvk.component.rtsmodels;

import lombok.Data;

import java.util.List;

@Data
public class GetTeacherRawScheduleResponse {
	private List<LessonCellMirror> rawSchedule;
}
