package com.a6raywa1cher.rescheduletsuvk.services.interfaces;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface FacultyService {
	CompletionStage<List<String>> getFacultiesList();

	CompletionStage<List<GetGroupsResponse.GroupInfo>> getGroupsList(String faculty);

	CompletionStage<GetGroupsResponse.GroupInfo> getGroupsStartsWith(String faculty, String group);
}
