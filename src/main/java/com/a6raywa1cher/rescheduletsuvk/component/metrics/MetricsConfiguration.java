package com.a6raywa1cher.rescheduletsuvk.component.metrics;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MetricsConfiguration {
	@Bean
	MeterRegistryCustomizer<?> meterRegistryCustomizer() {
		return (meter) -> meter.config().commonTags("bot", "vk");
	}
}
