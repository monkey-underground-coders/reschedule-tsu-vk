package com.a6raywa1cher.rescheduletsuvk.component.rtsmodels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class GetGroupsResponse {
	private List<GroupInfo> groups;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static final class GroupInfo {
		private String level;
		private String name;
		private Integer subgroups;
		private Integer course;
		private List<LessonCellMirror> lessonCells;
	}
}
