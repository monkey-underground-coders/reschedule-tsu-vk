package com.a6raywa1cher.rescheduletsuvk.stages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.RtsServerRestComponent;
import com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.GetScheduleForWeekResponse;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.LessonCellMirror;
import com.a6raywa1cher.rescheduletsuvk.component.rtsmodels.WeekSign;
import com.a6raywa1cher.rescheduletsuvk.dao.interfaces.UserInfoService;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils;
import com.a6raywa1cher.rescheduletsuvk.utils.VkKeyboardButton;
import com.a6raywa1cher.rescheduletsuvk.utils.VkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.GroupActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.a6raywa1cher.rescheduletsuvk.component.StageRouterComponent.ROUTE;
import static com.a6raywa1cher.rescheduletsuvk.stages.MainMenuStage.NAME;
import static com.a6raywa1cher.rescheduletsuvk.utils.CommonUtils.*;

@Component(NAME)
public class MainMenuStage implements Stage {
	public static final String NAME = "mainMenuStage";
	private static final String GET_SEVEN_DAYS = "Расписание на 7 дней";
	private static final String GET_TODAY_LESSONS = "Какие сегодня пары?";
	private static final String GET_TOMORROW_LESSONS = "Какие пары завтра (или в ПН)?";
	private static final String GET_NEXT_LESSON = "Какая следующая пара?";
	private static final String DROP_SETTINGS = "Изменить группу";
	private static final String GET_INFO = "Информация";
	private static final Logger log = LoggerFactory.getLogger(MainMenuStage.class);
	private VkApiClient vk;
	private GroupActor group;
	private StageRouterComponent stageRouterComponent;
	private RtsServerRestComponent restComponent;
	private UserInfoService service;

	@Autowired
	public MainMenuStage(VkApiClient vk, GroupActor group, StageRouterComponent stageRouterComponent,
	                     RtsServerRestComponent restComponent, UserInfoService service) {
		this.vk = vk;
		this.group = group;
		this.stageRouterComponent = stageRouterComponent;
		this.restComponent = restComponent;
		this.service = service;
	}

	private String getDefaultKeyboard() {
		ObjectMapper objectMapper = new ObjectMapper();
		String basicPayload = objectMapper.createObjectNode()
				.put(ROUTE, NAME)
				.toString();
		return VkUtils.createKeyboard(false, new int[]{1, 1, 1, 1, 2},
				new VkKeyboardButton(VkKeyboardButton.Color.PRIMARY, GET_SEVEN_DAYS, basicPayload),
				new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, GET_TODAY_LESSONS, basicPayload),
				new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, GET_NEXT_LESSON, basicPayload),
				new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, GET_TOMORROW_LESSONS, basicPayload),
				new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, DROP_SETTINGS, basicPayload),
				new VkKeyboardButton(VkKeyboardButton.Color.SECONDARY, GET_INFO, basicPayload)
		);
	}

	private void getSevenDays(UserInfo userInfo, ExtendedMessage message) {
		restComponent.getScheduleForWeek(userInfo.getFacultyId(), userInfo.getGroupId())
				.thenAccept(response -> {
					StringBuilder sb = new StringBuilder();
					boolean today = response.getSchedules().get(0).getDayOfWeek() == LocalDate.now().getDayOfWeek();
					for (GetScheduleForWeekResponse.Schedule schedule : response.getSchedules()) {
						sb.append(CommonUtils.convertLessonCells(schedule.getDayOfWeek(), schedule.getSign(),
								today, schedule.getCells().stream()
										.filter(cell -> cell.getSubgroup() == 0 || cell.getSubgroup().equals(userInfo.getSubgroup()))
										.collect(Collectors.toList()), false));
						today = false;
					}
					VkUtils.sendMessage(vk, group, message.getUserId(),
							sb.toString(), getDefaultKeyboard());
				})
				.exceptionally(e -> {
					log.error("Get seven days error\n" + message.toString() + "\n", e);
					return null;
				});
	}

	private void getTodayLessons(UserInfo userInfo, ExtendedMessage extendedMessage) {
		restComponent.getRawSchedule(userInfo.getFacultyId(), userInfo.getGroupId())
				.thenCombine(restComponent.getWeekSign(userInfo.getFacultyId()), (list, weekSignResult) -> {
					LocalDate localDate = LocalDate.now();
					List<LessonCellMirror> todayLessons = list.stream()
							.filter(cell -> cell.getDayOfWeek().equals(localDate.getDayOfWeek()))
							.filter(cell -> cell.getWeekSign().equals(WeekSign.ANY) ||
									cell.getWeekSign().equals(weekSignResult.getWeekSign()))
							.filter(cell -> cell.getSubgroup() == 0 || cell.getSubgroup().equals(userInfo.getSubgroup()))
							.collect(Collectors.toList());
					if (todayLessons.isEmpty()) {
						VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
								"Сегодня нет пар! Отдыхай, студент",
								getDefaultKeyboard());
					} else {
						VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
								CommonUtils.convertLessonCells(localDate.getDayOfWeek(),
										weekSignResult.getWeekSign(), true, todayLessons, true),
								getDefaultKeyboard());
					}
					return null;
				})
				.exceptionally(e -> {
					log.error("Get today lessons error\n" + extendedMessage.toString() + "\n", e);
					return null;
				});
	}

	private void getNextLesson(UserInfo userInfo, ExtendedMessage extendedMessage) {
		restComponent.getRawSchedule(userInfo.getFacultyId(), userInfo.getGroupId())
				.thenCombine(restComponent.getWeekSign(userInfo.getFacultyId()), (list, weekSignResult) -> {
					LocalDateTime localDateTime = LocalDateTime.now();
					Optional<LessonCellMirror> nextLesson = list.stream()
							.filter(cell -> cell.getDayOfWeek().equals(localDateTime.getDayOfWeek()))
							.filter(cell -> cell.getWeekSign().equals(WeekSign.ANY) ||
									cell.getWeekSign().equals(weekSignResult.getWeekSign()))
							.filter(cell -> cell.getStart().isAfter(localDateTime.toLocalTime()))
							.filter(cell -> cell.getSubgroup() == 0 || cell.getSubgroup().equals(userInfo.getSubgroup()))
							.findFirst();
					if (nextLesson.isEmpty()) {
						VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
								"Больше сегодня пар не ожидается",
								getDefaultKeyboard());
					} else {
						VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
								CommonUtils.convertLessonCell(nextLesson.get(), true, true),
								getDefaultKeyboard());
					}
					return null;
				})
				.exceptionally(e -> {
					log.error("Get next lesson error\n" + extendedMessage.toString() + "\n", e);
					return null;
				});
	}

	private void getTomorrowLessons(UserInfo userInfo, ExtendedMessage extendedMessage) {
		restComponent.getRawSchedule(userInfo.getFacultyId(), userInfo.getGroupId())
				.thenCombine(restComponent.getWeekSign(userInfo.getFacultyId()), (list, weekSignResult) -> {
					LocalDate now = LocalDate.now();
					WeekSign weekSign = weekSignResult.getWeekSign();
					boolean isDayAfterTomorrow = false;
					LocalDate localDate;
					switch (now.getDayOfWeek()) {
						case SATURDAY:
							localDate = now.plusDays(2);
							weekSign = WeekSign.inverse(weekSign);
							isDayAfterTomorrow = true;
							break;
						case SUNDAY:
							localDate = now.plusDays(1);
							weekSign = WeekSign.inverse(weekSign);
							break;
						default:
							localDate = now.plusDays(1);
							break;
					}
					LocalDate finalLocalDate = localDate;
					WeekSign finalWeekSign = weekSign;
					List<LessonCellMirror> todayLessons = list.stream()
							.filter(cell -> cell.getDayOfWeek().equals(finalLocalDate.getDayOfWeek()))
							.filter(cell -> cell.getWeekSign().equals(WeekSign.ANY) ||
									cell.getWeekSign().equals(finalWeekSign))
							.filter(cell -> cell.getSubgroup() == 0 || cell.getSubgroup().equals(userInfo.getSubgroup()))
							.collect(Collectors.toList());
					if (todayLessons.isEmpty()) {
						VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
								isDayAfterTomorrow ? "Послезавтра нет пар! Отдыхай, студент" :
										"Завтра нет пар! Отдыхай, студент",
								getDefaultKeyboard());
					} else {
						VkUtils.sendMessage(vk, group, extendedMessage.getUserId(),
								CommonUtils.convertLessonCells(finalLocalDate.getDayOfWeek(),
										finalWeekSign, false, todayLessons, true),
								getDefaultKeyboard());
					}
					return null;
				})
				.exceptionally(e -> {
					log.error("Get tomorrow lessons error\n" + extendedMessage.toString() + "\n", e);
					return null;
				});
	}

	private void dropSettings(UserInfo userInfo, ExtendedMessage message) {
		service.delete(userInfo);
		stageRouterComponent.routeMessage(message, WelcomeStage.NAME);
	}

	private void greeting(UserInfo userInfo, ExtendedMessage message) {
		VkUtils.sendMessage(vk, group, message.getUserId(),
				"Главное меню. Тут уютно, есть печеньки. " + COOKIES_EMOJI + '\n' +
						"Настроена " + userInfo.getGroupId() + " группа" +
						(userInfo.getSubgroup() != null ? ", " + userInfo.getSubgroup() + " подгруппа" : "") + '\n' +
						"Условные обозначения: \n" +
						CROSS_PAIR_EMOJI + " - пара у нескольких групп, \n" +
						SINGLE_SUBGROUP_EMOJI + " - пара только у одной из подгрупп.", getDefaultKeyboard());
	}

	@Override
	public void accept(ExtendedMessage message) {
		Optional<UserInfo> optionalUserInfo = service.getById(message.getUserId());
		if (optionalUserInfo.isEmpty()) {
			stageRouterComponent.routeMessage(message, WelcomeStage.NAME);
			return;
		}
		if (message.getPayload() != null) {
			switch (message.getBody()) {
				case GET_SEVEN_DAYS:
					getSevenDays(optionalUserInfo.get(), message);
					break;
				case GET_TODAY_LESSONS:
					getTodayLessons(optionalUserInfo.get(), message);
					break;
				case GET_TOMORROW_LESSONS:
					getTomorrowLessons(optionalUserInfo.get(), message);
					break;
				case GET_NEXT_LESSON:
					getNextLesson(optionalUserInfo.get(), message);
					break;
				case DROP_SETTINGS:
					dropSettings(optionalUserInfo.get(), message);
					break;
				case GET_INFO:
				default:
					greeting(optionalUserInfo.get(), message);
			}
		} else greeting(optionalUserInfo.get(), message);
	}
}
