package com.a6raywa1cher.rescheduletsuvk.component;

import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetFacultiesResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.config.AppConfigProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

@Component
public class RtsServerRestComponent {
	private static final Logger log = LoggerFactory.getLogger(RtsServerRestComponent.class);
	private ExecutorService executor;
	private URI rts;

	@Autowired
	public RtsServerRestComponent(AppConfigProperties properties) {
		this.executor = new ForkJoinPool();
		this.rts = URI.create(properties.getRtsUrl());
	}

	private String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			log.error("wtf??", e);
			throw new RuntimeException(e);
		}
	}

	private <T> CompletionStage<T> request(String url, Class<T> tClass) {
		return CompletableFuture.supplyAsync(() -> {
			ObjectMapper objectMapper = new ObjectMapper();
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

	public CompletionStage<GetFacultiesResponse> getFaculties() {
		return request("faculties", GetFacultiesResponse.class);
	}

	public CompletionStage<GetGroupsResponse> getGroups(String facultyId) {
		return request(encodeValue(facultyId) + "/groups", GetGroupsResponse.class);
	}
}
