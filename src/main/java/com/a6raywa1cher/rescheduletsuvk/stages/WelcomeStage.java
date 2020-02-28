package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.router.MessageResponse;
import com.a6raywa1cher.rescheduletsuvk.component.router.RTMessageMapping;
import com.a6raywa1cher.rescheduletsuvk.component.router.RTStage;
import com.a6raywa1cher.rescheduletsuvk.config.stringconfigs.WelcomeStageStringsConfigProperties;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.ScheduleService;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@RTStage(textQuery = "/")
public class WelcomeStage {
	private UserInfoService userInfoService;
	private WelcomeStageStringsConfigProperties properties;
	private ScheduleService scheduleService;

	@Autowired
	public WelcomeStage(UserInfoService userInfoService, WelcomeStageStringsConfigProperties properties,
	                    ScheduleService scheduleService) {
		this.userInfoService = userInfoService;
		this.properties = properties;
		this.scheduleService = scheduleService;
	}


	@RTMessageMapping("/")
	public CompletionStage<MessageResponse> accept(ExtendedMessage message) {
		Optional<UserInfo> optional = userInfoService.getById(message.getUserId());
		if (optional.isPresent()) {
			UserInfo userInfo = optional.get();
			return scheduleService.getRawSchedule(userInfo.getFacultyId(), userInfo.getGroupId())
					.thenApply(o -> MessageResponse.builder()
							.redirectTo("/home/")
							.build())
					.exceptionally(e -> MessageResponse.builder()
							.message(properties.getWelcome())
							.redirectTo("/configure/step1")
							.build());
		} else {
			return CompletableFuture.completedStage(
					MessageResponse.builder()
							.message(properties.getWelcome())
							.redirectTo("/configure/step1")
							.build()
			);
		}
	}

}
