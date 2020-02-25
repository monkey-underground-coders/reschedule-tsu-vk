package com.a6raywa1cher.rescheduletsuvk.component.router;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.component.MetricsRegistrar;
import com.a6raywa1cher.rescheduletsuvk.component.messageoutput.MessageOutput;
import com.a6raywa1cher.rescheduletsuvk.filterstages.FilterStage;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Sentry;
import io.sentry.event.Breadcrumb;
import io.sentry.event.BreadcrumbBuilder;
import io.sentry.event.EventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
	@Lazy
	private MetricsRegistrar metricsRegistrar;

	@Autowired
	public DefaultMessageRouter(MessageOutput messageOutput, UserInfoService userInfoService) {
		this.userInfoService = userInfoService;
		this.filterStages = new ArrayList<>();
		this.pathToMappingMap = new HashMap<>();
		executor = new ForkJoinPool();
		this.userStateMap = new HashMap<>();
		this.messageOutput = messageOutput;
	}

	private static void invocationErrorRender(RequestInfo requestInfo, String errorMessage, UserState userState, Throwable e) {
		log.error(errorMessage, e);
		Sentry.capture(new EventBuilder()
				.withMessage(errorMessage)
				.withTimestamp(new Date())
				.withExtra("req-uuid", requestInfo.getUuid())
				.withBreadcrumbs(new LinkedList<>(requestInfo.getBreadcrumbList()))
				.withExtra("userState", userState.toString())
		);
	}

	public void routeMessageToPath(ExtendedMessage message, String path0, RequestInfo requestInfo) {
		String path = normalizePath(path0);
		metricsRegistrar.registerPath(path);
		requestInfo.getBreadcrumbList().add(new BreadcrumbBuilder()
				.setMessage(path)
				.setTimestamp(new Date())
				.setCategory("route")
				.setLevel(Breadcrumb.Level.INFO)
				.setType(Breadcrumb.Type.NAVIGATION)
				.build()
		);
		MappingMethodInfo mappingMethodInfo = pathToMappingMap.get(path);
		UserState userState = getUserState(message);
		if (mappingMethodInfo == null) {
			throw new IllegalArgumentException("Wrong path " + path);
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
//			if (MessageResponse.class.isAssignableFrom(returnClass)) {
//				MessageResponse messageResponse = (MessageResponse) o;
//				acceptMessageResponse(message, mappingMethodInfo, messageResponse, userState, traceRoute);
//			} else
			if (CompletionStage.class.isAssignableFrom(returnClass) || MessageResponse.class.isAssignableFrom(returnClass)) {
				CompletionStage<?> completionStage;
				if (CompletionStage.class.isAssignableFrom(returnClass)) {
					completionStage = (CompletionStage<?>) o;
				} else {
					completionStage = CompletableFuture.completedStage((MessageResponse) o);
				}
				completionStage
						.thenApplyAsync(x -> x, executor)
						.exceptionally(e -> {
							invocationErrorRender(requestInfo, String.format("Error during invocation path \"%s\", user %d",
									path, userState.getUserId()), userState, e);
							if (!path.equals(mappingMethodInfo.getExceptionRedirect())) {
								MessageResponse messageResponse = MessageResponse.builder().redirectTo(mappingMethodInfo.getExceptionRedirect()).build();
								acceptMessageResponse(message, mappingMethodInfo, messageResponse, userState, requestInfo);
							}
							return null;
						})
						.thenAcceptAsync(o1 -> {
							if (o1 == null) {
								return;
							}
							if (!MessageResponse.class.isAssignableFrom(o1.getClass())) {
								log.error("Invalid return type! Path: {}", path);
							} else {
								MessageResponse messageResponse = (MessageResponse) o1;
								acceptMessageResponse(message, mappingMethodInfo, messageResponse, userState, requestInfo);
							}
						}, executor)
						.exceptionally(e -> {
							invocationErrorRender(requestInfo, String.format("Error during invocation path \"%s\", user %d",
									path, userState.getUserId()), userState, e);
							return null;
						});
			} else {
				throw new RuntimeException("Invalid return type! Path: " + path);
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invocation error", e);
		} catch (InvocationTargetException e) {
			invocationErrorRender(requestInfo, String.format("Error during invocation path \"%s\", user %d",
					path, userState.getUserId()), userState, e);
		}
	}

	private void acceptMessageResponse(ExtendedMessage input, MappingMethodInfo processor, MessageResponse out,
	                                   UserState userState, RequestInfo requestInfo) {
		MessageResponse output = requestInfo.resolveMessageResponse(out).getPreviousMessageResponse();
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
		if (output.getRedirectTo() != null) {
			requestInfo.getPreviousMessageResponse().getContainerChanges().clear();
			executor.execute(() -> {
				try {
					routeMessageToPath(input, output.getRedirectTo(), requestInfo);
				} catch (Exception e) {
					invocationErrorRender(requestInfo, "Catched exception during path routing", userState, e);
					routeMessageToPath(input, "/", requestInfo);
				}
			});
		} else {
			output.getMessages().forEach(m ->
					messageOutput.sendMessage(output.getTo() == null ? userState.getUserId() : output.getTo(),
							m, output.getKeyboard()));
			metricsRegistrar.registerTimeConsumed(System.currentTimeMillis() - requestInfo.getStart());
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
		executor.execute(() -> $routeMessage(message, new RequestInfo()));
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

	private void $routeMessage(ExtendedMessage message, RequestInfo requestInfo) {
		UserState userState = getUserState(message);
		try {
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
				routeMessageToPath(message, optionalPath.get(), requestInfo);
			} else if (userState.getTextQueryConsumerPath() != null) {
				routeMessageToPath(message, userState.getTextQueryConsumerPath(), requestInfo);
			} else {
				routeMessageToPath(message, "/", requestInfo);
			}
		} catch (Exception e) {
			invocationErrorRender(requestInfo, "Catched exception during path routing", userState, e);
			routeMessageToPath(message, "/", requestInfo);
		}
	}

	private UserState getUserState(ExtendedMessage message) {
		int userId = message.getUserId();
		return userStateMap.computeIfAbsent(userId, UserState::new);
	}
}
