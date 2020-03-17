package com.a6raywa1cher.rescheduletsuvk.component.metrics;

import com.a6raywa1cher.rescheduletsuvk.component.rts.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetGroupsResponse;
import com.a6raywa1cher.rescheduletsuvk.dao.UserInfoRepository;
import com.a6raywa1cher.rescheduletsuvk.dao.submodel.FacultyGroupCount;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MetricsRegistrar {
	private MeterRegistry meterRegistry;
	private Set<Integer> users;
	private Timer timer;
	private UserInfoRepository userInfoRepository;
	private RtsServerRestComponent rtsServerRestComponent;

	public MetricsRegistrar(MeterRegistry meterRegistry, UserInfoRepository userInfoRepository,
	                        RtsServerRestComponent rtsServerRestComponent) {
		this.meterRegistry = meterRegistry;
		this.userInfoRepository = userInfoRepository;
		this.rtsServerRestComponent = rtsServerRestComponent;
		this.users = new HashSet<>();
	}

	@PostConstruct
	public void init() {
		if (meterRegistry != null) {
			this.timer = meterRegistry.timer("rt.time.per.message");
			Gauge.builder("rt.users.today", () -> users.size())
					.register(meterRegistry);
			updateUsersStats();
		}
	}

	public void registerPath(String path) {
		meterRegistry.counter("rt.path.usage", "path", path).increment();
	}

	public void registerUserCall(Integer userId) {
		users.add(userId);
	}

	@Scheduled(cron = "0 0 4 * * *")
	public void updateTodayUsersStats() {
		users.clear();
	}

	@Scheduled(cron = "0 0 4 * * *")
	@EventListener(ApplicationReadyEvent.class)
	@Transactional(readOnly = true)
	public void updateUsersStats() {
		Set<FacultyGroupCount> list = new HashSet<>(userInfoRepository.getCoursesCount());
		Set<String> faculties = list.stream()
				.map(FacultyGroupCount::getFacultyId)
				.collect(Collectors.toUnmodifiableSet());

		Map<String, Map<String, Long>> facultyToGroups = new HashMap<>();
		faculties.forEach(faculty -> facultyToGroups.put(faculty, new HashMap<>()));
		list.forEach(fgc -> facultyToGroups.get(fgc.getFacultyId()).put(fgc.getGroupId(), fgc.getCount()));

		List<CompletableFuture<Set<CourseInfo>>> completableFutures = new ArrayList<>(faculties.size());
		for (String faculty : faculties) {
			CompletionStage<GetGroupsResponse> completionStage = rtsServerRestComponent.getGroups(faculty);
			CompletionStage<Set<CourseInfo>> convertToCourseInfoSet = completionStage
					.thenApply(response -> {
						Map<String, Long> appliedGroups = facultyToGroups.get(faculty);
						return response.getGroups().stream()
								.filter(gi -> appliedGroups.containsKey(gi.getName()))
								.map(gi -> new CourseInfo(faculty, gi.getLevel(), gi.getCourse(), appliedGroups.get(gi.getName())))
								.collect(Collectors.toSet());
					})
					.exceptionally(e -> {
						log.error("Error during updateUsersStats", e);
						return null;
					});
			completableFutures.add(convertToCourseInfoSet.toCompletableFuture());
		}
		CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[]{}))
				.thenAccept(v -> {
					completableFutures.stream()
							.map(cf -> cf.getNow(null))
							.filter(Objects::nonNull)
							.flatMap(Collection::stream)
							.forEach(info -> meterRegistry.gauge("rt.users.info",
									List.of(
											Tag.of("faculty", info.getFaculty()),
											Tag.of("course", Integer.toString(info.getCourse())),
											Tag.of("program", info.getProgram())
									),
									info.getCount()
							));
				});
	}

	public void registerTimeConsumed(long millisecond) {
		timer.record(millisecond, TimeUnit.MILLISECONDS);
	}
}
