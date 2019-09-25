package com.a6raywa1cher.rescheduletsuvk.services.impls;

import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetFacultiesResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.FacultyService;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Service
public class FacultyServiceImpl implements FacultyService {
	private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);
	private RtsServerRestComponent restComponent;

	public FacultyServiceImpl(RtsServerRestComponent restComponent) {
		this.restComponent = restComponent;
	}

	@Override
	public CompletionStage<List<String>> getFacultiesList() {
		return restComponent.getFaculties().thenApply(GetFacultiesResponse::getFaculties)
				.exceptionally(e -> {
					log.error("Get faculties error", e);
					Sentry.capture(e);
					return new ArrayList<>();
				});
	}

	@Override
	public CompletionStage<List<GetGroupsResponse.GroupInfo>> getGroupsList(String faculty) {
		return restComponent.getGroups(faculty).thenApply(GetGroupsResponse::getGroups)
				.exceptionally(e -> {
					log.error("Get groups error", e);
					Sentry.capture(e);
					return new ArrayList<>();
				});
	}

	@Override
	public CompletionStage<GetGroupsResponse.GroupInfo> getGroupsStartsWith(String faculty, String group) {
		return this.getGroupsList(faculty)
				.thenApply(response -> response.stream()
						.filter(gi -> gi.getName().contains(group)).findFirst().orElse(null))
				.exceptionally(e -> {
					log.error("Find group error", e);
					Sentry.capture(e);
					return null;
				});
	}
}
