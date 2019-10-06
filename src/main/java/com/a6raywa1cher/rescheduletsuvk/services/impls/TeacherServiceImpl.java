package com.a6raywa1cher.rescheduletsuvk.services.impls;

import com.a6raywa1cher.rescheduletsuvk.component.backend.BackendComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetTeachersResponse;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.TeacherService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletionStage;

@Service
public class TeacherServiceImpl implements TeacherService {
	private BackendComponent restComponent;

	public TeacherServiceImpl(BackendComponent restComponent) {
		this.restComponent = restComponent;
	}

	@Override
	public CompletionStage<List<String>> findTeacher(String teacherName) {
		return restComponent.findTeacher(teacherName)
				.thenApply(GetTeachersResponse::getTeachers);
	}
}
