package com.a6raywa1cher.rescheduletsuvk.component.rtsmodels;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GetScheduleForWeekResponse {
	private List<Schedule> schedules = new ArrayList<>();
}
