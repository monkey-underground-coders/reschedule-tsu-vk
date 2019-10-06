package com.a6raywa1cher.rescheduletsuvk.services.interfaces;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface TeacherService {
	CompletionStage<List<String>> findTeacher(String teacherName);
}
