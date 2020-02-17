package com.a6raywa1cher.rescheduletsuvk.component.rtsmodels;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LessonCellMirror {
	private WeekSign weekSign;

	private String fullSubjectName;

	private String shortSubjectName;

	private String teacherName;

	private String teacherTitle;

	private DayOfWeek dayOfWeek;

	private Integer columnPosition;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime start;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
	private LocalTime end;

	private String auditoryAddress;

	private Integer course;

	private String group;

	private Integer subgroup;

	private Boolean crossPair;

	private String faculty;

	private Boolean userMade;
}
