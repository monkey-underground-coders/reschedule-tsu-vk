package com.a6raywa1cher.rescheduletsuvk.component.backend;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.*;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.UrlEscapers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

@Component
public class RtsServerRestBackendComponent implements BackendComponent {
	private static final Logger log = LoggerFactory.getLogger(RtsServerRestBackendComponent.class);
	private ExecutorService executor;
	private URI rts;

	@Autowired
	public RtsServerRestBackendComponent(AppConfigProperties properties) {
		this.executor = new ForkJoinPool();
		this.rts = URI.create(properties.getRtsUrl());
	}

	private String encodeValue(String value) {
		return UrlEscapers.urlFragmentEscaper().escape(value);
	}

	private <T> CompletionStage<T> request(String url, Class<T> tClass) {
		return CompletableFuture.supplyAsync(() -> {
			ObjectMapper objectMapper = new ObjectMapper()
					.registerModule(new JavaTimeModule());
			try {
				return objectMapper.readValue(this.rts.resolve(url).toURL(), tClass);
			} catch (MalformedURLException e) {
				log.error("Bad url?", e);
				throw new RuntimeException(e);
			} catch (IOException e) {
				log.error(String.format("IOException during call to %s", url), e);
				throw new RuntimeException(e);
			}
		}, executor);
	}

	private <T> CompletionStage<T> request(String url, TypeReference<T> typeReference) {
		return CompletableFuture.supplyAsync(() -> {
			ObjectMapper objectMapper = new ObjectMapper()
					.registerModule(new JavaTimeModule());
			try {
				return objectMapper.readValue(this.rts.resolve(url).toURL(), typeReference);
			} catch (MalformedURLException e) {
				log.error("Bad url?", e);
				throw new RuntimeException(e);
			} catch (IOException e) {
				log.error(String.format("IOException during call to %s", url), e);
				throw new RuntimeException(e);
			}
		}, executor);
	}

	@Override
	public CompletionStage<GetFacultiesResponse> getFaculties() {
		return request("faculties", GetFacultiesResponse.class);
	}

	@Override
	public CompletionStage<GetScheduleForWeekResponse> getScheduleForWeek(String facultyId, String groupId) {
		return request("faculties/" + encodeValue(facultyId) + "/groups/" + encodeValue(groupId) + "/week",
				GetScheduleForWeekResponse.class);
	}

	@Override
	public CompletionStage<GetScheduleForWeekResponse> getScheduleForWeek(String facultyId, String groupId, LocalDate date) {
		return request("faculties/" + encodeValue(facultyId) + "/groups/" + encodeValue(groupId) + "/week" +
						"?day=" + date.format(DateTimeFormatter.ISO_DATE),
				GetScheduleForWeekResponse.class);
	}

	@Override
	public CompletionStage<GetGroupsResponse> getGroups(String facultyId) {
		return request("faculties/" + encodeValue(facultyId) + "/groups", GetGroupsResponse.class);
	}

	@Override
	public CompletionStage<List<LessonCellMirror>> getRawSchedule(String facultyId, String groupId) {
		return request("faculties/" + encodeValue(facultyId) + "/groups/" + encodeValue(groupId),
				new TypeReference<>() {
				}
		);
	}

	@Override
	public CompletionStage<GetWeekSignResponse> getWeekSign(String facultyId) {
		return request("faculties/" + encodeValue(facultyId) + "/week_sign", GetWeekSignResponse.class);
	}

	@Override
	public CompletionStage<GetWeekSignResponse> getWeekSign(String facultyId, LocalDate localDate) {
		return request("faculties/" + encodeValue(facultyId) + "/week_sign?day="
				+ localDate.format(DateTimeFormatter.ISO_DATE), GetWeekSignResponse.class);
	}

	@Override
	public CompletionStage<GetTeachersResponse> findTeacher(String teacherName) {
		return request("teachers/find/" + encodeValue(teacherName), GetTeachersResponse.class);
	}

	@Override
	public CompletionStage<GetScheduleOfTeacherForWeekResponse> getTeacherWeekSchedule(String teacherName) {
		return request("teachers/" + encodeValue(teacherName) + "/week", GetScheduleOfTeacherForWeekResponse.class);
	}

	@Override
	public CompletionStage<GetTeacherRawScheduleResponse> getTeacherRawSchedule(String teacherName) {
		return request("teachers/" + encodeValue(teacherName), GetTeacherRawScheduleResponse.class);
	}
}
