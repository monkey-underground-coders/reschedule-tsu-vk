package com.a6raywa1cher.rescheduletsuvk.component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class MetricsRegistrar {
	//	@Autowired
	private MeterRegistry meterRegistry;
	private Timer timer;
	private Set<Integer> users;

	@Autowired
	public MetricsRegistrar(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.users = new HashSet<>();
	}

	@PostConstruct
	public void init() {
		timer = meterRegistry.timer("rtvk_time_per_message");
		Gauge.builder("rtvk_unique_users", () -> users.size())
				.register(meterRegistry);
	}

	public void registerPath(String path) {
		meterRegistry.counter("rtvk_path_usage", "path", path).increment();
	}

	public void registerUserCall(Integer userId) {
		users.add(userId);
	}

	@Scheduled(cron = "0 0 4 * * *")
	public void updateUserStats() {
		users.clear();
	}

	public void registerTimeConsumed(long millisecond) {
		timer.record(millisecond, TimeUnit.MILLISECONDS);
	}
}
