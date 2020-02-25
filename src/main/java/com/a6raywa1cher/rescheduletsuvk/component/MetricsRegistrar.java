package com.a6raywa1cher.rescheduletsuvk.component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
public class MetricsRegistrar {
	//	@Autowired
	private MeterRegistry meterRegistry;
	private Timer timer;

	@Autowired
	public MetricsRegistrar(MeterRegistry meterRegistry) {

		this.meterRegistry = meterRegistry;
	}

	@PostConstruct
	public void init() {
		timer = meterRegistry.timer("rtvk_time_per_message");
	}

	public void registerPath(String path) {
		meterRegistry.counter("rtvk_path_usage", "path", path).increment();
	}

	public void registerTimeConsumed(long millisecond) {
		timer.record(millisecond, TimeUnit.MILLISECONDS);
	}
}
