package com.a6raywa1cher.rescheduletsuvk.component.rts;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.*;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.UrlEscapers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

@Component
public class RtsServerRestComponent {
	private static final Logger log = LoggerFactory.getLogger(RtsServerRestComponent.class);
	private ExecutorService executor;
	private RestTemplate restTemplate;
	private URI rts;

	@Autowired
	public RtsServerRestComponent(AppConfigProperties properties) {
		this.executor = new ForkJoinPool();
		this.rts = URI.create(properties.getRtsUrl());
		this.restTemplate = new RestTemplate();
	}

	private String encodeValue(String value) {
		return UrlEscapers.urlFragmentEscaper().escape(value);
	}

	private <T> CompletionStage<T> request(String url, Class<T> tClass) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				URI src = this.rts.resolve(url);
				ResponseEntity<T> result;
				try {
					result = restTemplate.getForEntity(src, tClass);
				} catch (HttpClientErrorException.NotFound e) {
					throw new NotFoundException(e);
				}
				if (result.getStatusCode().is2xxSuccessful()) {
					return result.getBody();
				} else {
					log.error("Unexpected result: " + result.toString());
					throw new RuntimeException("Unexpected result: " + result.toString());
				}
			} catch (RestClientException e) {
				log.error(String.format("RestClientException during call to %s", url), e);
				throw new RuntimeException(e);
			}
		}, executor);
	}

	private <T> CompletionStage<T> request(String url, TypeReference<T> typeReference) {
		return CompletableFuture.supplyAsync(() -> {
			ObjectMapper objectMapper = new ObjectMapper()
					.registerModule(new JavaTimeModule());
			try {
				URI src = this.rts.resolve(url);
				ResponseEntity<String> result;
				try {
					result = restTemplate.getForEntity(src, String.class);
				} catch (HttpClientErrorException.NotFound e) {
					throw new NotFoundException(e);
				}
				if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
					return objectMapper.readValue(result.getBody(), typeReference);
				} else {
					log.error("Unexpected result: " + result.toString());
					throw new RuntimeException("Unexpected result: " + result.toString());
				}
			} catch (RestClientException e) {
				log.error(String.format("RestClientException during call to %s", url), e);
				throw new RuntimeException(e);
			} catch (JsonProcessingException e) {
				log.error(String.format("JsonProcessingException during call to %s", url), e);
				throw new RuntimeException(e);
			}
		}, executor);
	}

	public CompletionStage<GetFacultiesResponse> getFaculties() {
		return request("faculties", GetFacultiesResponse.class);
	}

	public CompletionStage<GetScheduleForWeekResponse> getScheduleForWeek(String facultyId, String groupId) {
		return request("faculties/" + encodeValue(facultyId) + "/groups/" + encodeValue(groupId) + "/week",
				GetScheduleForWeekResponse.class);
	}

	public CompletionStage<GetScheduleForWeekResponse> getScheduleForWeek(String facultyId, String groupId, LocalDate date) {
		return request("faculties/" + encodeValue(facultyId) + "/groups/" + encodeValue(groupId) + "/week" +
						"?day=" + date.format(DateTimeFormatter.ISO_DATE),
				GetScheduleForWeekResponse.class);
	}

	public CompletionStage<GetGroupsResponse> getGroups(String facultyId) {
		return request("faculties/" + encodeValue(facultyId) + "/groups", GetGroupsResponse.class);
	}

	public CompletionStage<List<LessonCellMirror>> getRawSchedule(String facultyId, String groupId) {
		return request("faculties/" + encodeValue(facultyId) + "/groups/" + encodeValue(groupId),
				new TypeReference<>() {
				}
		);
	}

	public CompletionStage<GetWeekSignResponse> getWeekSign(String facultyId) {
		return request("faculties/" + encodeValue(facultyId) + "/week_sign", GetWeekSignResponse.class);
	}

	public CompletionStage<GetWeekSignResponse> getWeekSign(String facultyId, LocalDate localDate) {
		return request("faculties/" + encodeValue(facultyId) + "/week_sign?day="
				+ localDate.format(DateTimeFormatter.ISO_DATE), GetWeekSignResponse.class);
	}

	public CompletionStage<GetTeachersResponse> findTeacher(String teacherName) {
		return request("teachers/find/" + encodeValue(teacherName), GetTeachersResponse.class);
	}

	public CompletionStage<GetScheduleOfTeacherForWeekResponse> getTeacherWeekSchedule(String teacherName) {
		return request("teachers/" + encodeValue(teacherName) + "/week", GetScheduleOfTeacherForWeekResponse.class);
	}
}
