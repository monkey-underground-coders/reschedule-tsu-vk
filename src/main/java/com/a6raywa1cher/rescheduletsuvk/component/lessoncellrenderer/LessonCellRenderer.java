package com.a6raywa1cher.rescheduletsuvk.component.lessoncellrenderer;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;

import java.time.DayOfWeek;
import java.util.Collection;

public interface LessonCellRenderer {
	String emojifyDigit(int digit);

	String reduceFullName(String fullName);

	String convertLessonCells(DayOfWeek dayOfWeek, WeekSign weekSign, boolean today,
	                          Collection<LessonCellMirror> lessonCellMirrors, boolean detailed);

	String convertLessonCells(DayOfWeek dayOfWeek, WeekSign weekSign, boolean today,
	                          Collection<LessonCellMirror> lessonCellMirrors, boolean detailed,
	                          boolean showTeachers, boolean showGroups, boolean showEmoji);

	String reduceLessonCells(Collection<LessonCellMirror> mirrors, boolean today, boolean detailed,
	                         boolean showTeachers, boolean showGroups, boolean showEmoji);

	String convertLessonCell(LessonCellMirror mirror, boolean today, boolean detailed);

	String convertLessonCell(LessonCellMirror mirror, boolean today, boolean detailed,
	                         boolean showTeachers, boolean showGroups, boolean showEmoji);
}
