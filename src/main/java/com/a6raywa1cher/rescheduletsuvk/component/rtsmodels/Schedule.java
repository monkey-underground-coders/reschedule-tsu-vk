package com.a6raywa1cher.rescheduletsuvk.component.rtsmodels;

import lombok.Data;

import java.time.DayOfWeek;
import java.util.List;

@Data
public class Schedule {
	private DayOfWeek dayOfWeek;
	private WeekSign weekSign;
	private List<LessonCellMirror> cells;
}
