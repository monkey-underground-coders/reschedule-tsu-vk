package com.a6raywa1cher.rescheduletsuvk.services.submodels;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DifferenceBetweenSubgroups {
	private LessonCellMirror first;

	private int firstSubgroup;

	private LessonCellMirror second;

	private int secondSubgroup;
}
