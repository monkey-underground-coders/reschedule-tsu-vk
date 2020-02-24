package com.a6raywa1cher.rescheduletsuvk.component.router;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.filterstages.FilterStage;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import static com.a6raywa1cher.rescheduletsuvk.component.router.PathMethods.normalizePath;

@Component
public class DefaultMessageRouter implements MessageRouter {
	private static final Logger log = LoggerFactory.getLogger(DefaultMessageRouter.class);
	private final List<FilterStage> filterStages;
	private final Executor executor;
	private Map<String, MappingMethodInfo> pathToMappingMap;
	private Map<Integer, UserState> userStateMap;
	private MessageOutput messageOutput;
	private UserInfoService userInfoService;

	@Autowired
	public DefaultMessageRouter(MessageOutput messageOutput, UserInfoService userInfoService) {
		this.userInfoService = userInfoService;
		this.filterStages = new ArrayList<>();
		this.pathToMappingMap = new HashMap<>();
		executor = new ForkJoinPool();
		this.userStateMap = new HashMap<>();
		this.messageOutput = messageOutput;
	}

	public void routeMessageToPath(ExtendedMessage message, String path0) {
		String path = normalizePath(path0);
		MappingMethodInfo mappingMethodInfo = pathToMappingMap.get(path);
		UserState userState = getUserState(message);
		if (mappingMethodInfo == null) {
			return;
		}
		Method provider = mappingMethodInfo.getInfoProvider();
		Object[] bakedParameters = new Object[provider.getParameterCount()];
		Parameter[] parameters = provider.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			Class<?> clazz = parameter.getType();
			if (parameter.isAnnotationPresent(RTContainerEntity.class)) {
				RTContainerEntity rtContainerEntity = parameter.getAnnotation(RTContainerEntity.class);
				String label = rtContainerEntity.value().equals("") ? parameter.getName() : rtContainerEntity.value();
				Object containerVal = userState.getContainer().getOrDefault(label, null);
				if (containerVal == null && !rtContainerEntity.nullable()) {
					throw new RuntimeException("No corresponding value in container: " + label);
				}
				bakedParameters[i] = containerVal;
			} else if (clazz == ExtendedMessage.class) {
				bakedParameters[i] = message;
			} else if (clazz == UserInfo.class) {
				bakedParameters[i] = userInfoService.getById(userState.getUserId()).orElse(null);
			} else {
				throw new IllegalArgumentException("Unknown parameter #" + i + " in " + provider.getDeclaringClass());
			}
		}
		try {
			Object o = mappingMethodInfo.getMethodToCall().invoke(mappingMethodInfo.getBean(), bakedParameters);
			if (o == null) return;
			Class<?> returnClass = o.getClass();
			if (MessageResponse.class.isAssignableFrom(returnClass)) {
				MessageResponse messageResponse = (MessageResponse) o;
				acceptMessageResponse(message, mappingMethodInfo, messageResponse, userState);
			} else if (CompletionStage.class.isAssignableFrom(returnClass)) {
				CompletionStage<?> completionStage = (CompletionStage<?>) o;
				completionStage
						.thenAcceptAsync(o1 -> {
							if (o1 == null) {
								return;
							}
							if (!MessageResponse.class.isAssignableFrom(o1.getClass())) {
								log.error("Invalid return type! Path: {}", path);
							} else {
								MessageResponse messageResponse = (MessageResponse) o1;
								acceptMessageResponse(message, mappingMethodInfo, messageResponse, userState);
							}
						}, executor)
						.exceptionally(e -> {
							log.error("Error during invocation path " + path + ", user " + userState.getUserId(), e);
							Sentry.capture(e);
							return null;
						});
			} else {
				throw new RuntimeException("Invalid return type! Path: " + path);
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invocation error", e);
		} catch (InvocationTargetException e) {
			log.error("Error during invocation path " + path + ", user " + userState.getUserId(), e);
			throw new RuntimeException("Invocation error", e);
		}
	}

	private void acceptMessageResponse(ExtendedMessage input, MappingMethodInfo processor, MessageResponse output, UserState userState) {
		if (output.getTextQueryParserPath() != null) {
			userState.setTextQueryConsumerPath(output.getTextQueryParserPath());
		} else if (processor.getDefaultTextQueryParserPath() != null) {
			userState.setTextQueryConsumerPath(processor.getDefaultTextQueryParserPath());
		}
		output.getContainerChanges().forEach((label, val) -> {
			Map<String, Object> targetContainer = userState.getContainer();
			if (val == null) {
				targetContainer.remove(label);
			} else {
				targetContainer.put(label, val);
			}
		});
//		if (output.getMessage() != null) {
//			messageOutput.sendMessage(output.getTo() == null ? userState.getUserId() : output.getTo(),
//					output.getMessage(), output.getKeyboard());
//		}
		output.getMessages().forEach(m ->
				messageOutput.sendMessage(output.getTo() == null ? userState.getUserId() : output.getTo(),
						m, output.getKeyboard()));
		if (output.getRedirectTo() != null) {
			executor.execute(() -> routeMessageToPath(input, output.getRedirectTo()));
		}
	}

	@Override
	public void addFilter(FilterStage filterStage) {
		this.filterStages.add(filterStage);
		log.info("Putted filter {}", filterStage.getClass().getSimpleName());
	}

	@Override
	public void addMapping(MappingMethodInfo mappingMethodInfo) {
		pathToMappingMap.put(mappingMethodInfo.getMappingPath(), mappingMethodInfo);
		log.info("Added mapping: {} -> {}", mappingMethodInfo.getMappingPath(), mappingMethodInfo.getInfoProvider().toGenericString());
	}

	public void routeMessage(ExtendedMessage message) {
		executor.execute(() -> $routeMessage(message));
	}

	private Optional<String> tryGatherPath(ExtendedMessage message) {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			JsonNode jsonNode = objectMapper.readTree(message.getPayload());
			return Optional.of(jsonNode.get(ROUTE).asText());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private void $routeMessage(ExtendedMessage message) {
		try {
			UserState userState = getUserState(message);
			for (FilterStage filterStage : filterStages) {
				message = filterStage.process(message);
				if (message == null) {
					log.info("FilterStage {} stopped processing of {}'s message",
							filterStage.getClass().toString(), userState.getUserId());
					return;
				}
			}
			Optional<String> optionalPath = tryGatherPath(message);
			if (optionalPath.isPresent()) {
				routeMessageToPath(message, optionalPath.get());
			} else if (userState.getTextQueryConsumerPath() != null) {
				routeMessageToPath(message, userState.getTextQueryConsumerPath());
			} else {
				routeMessageToPath(message, "/");
			}
		} catch (Exception e) {
			log.error("Catched exception", e);
			Sentry.capture(e);
			routeMessageToPath(message, "/");
		}
	}

	private UserState getUserState(ExtendedMessage message) {
		int userId = message.getUserId();
		return userStateMap.computeIfAbsent(userId, UserState::new);
	}
}
