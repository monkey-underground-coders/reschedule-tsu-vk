package com.a6raywa1cher.rescheduletsuvk.component.router;

import com.a6raywa1cher.rescheduletsuvk.filterstages.FilterStage;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import static com.a6raywa1cher.rescheduletsuvk.component.router.PathMethods.resolve;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class StageBeanPostProcessor implements BeanPostProcessor {
	@Autowired
	@Lazy
	private MessageRouter messageRouter;

	private Map<String, Class<?>> beanMap = new HashMap<>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		Class<?> aClass = bean.getClass();
		if (aClass.isAnnotationPresent(RTStage.class) || FilterStage.class.isAssignableFrom(aClass)) {
			beanMap.put(beanName, aClass);
		}
		return bean;
	}

	private void checkMethod(Method method) {
		if (!MessageResponse.class.isAssignableFrom(method.getReturnType()) &&
			!CompletionStage.class.isAssignableFrom(method.getReturnType())) {
			throw new RuntimeException(String.format("Invalid return type of %s, class %s",
				method.getName(), method.getDeclaringClass().getName()));
		}
	}

	@SneakyThrows
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Class<?> aClass = beanMap.get(beanName);
		if (aClass != null) {
			if (FilterStage.class.isAssignableFrom(aClass)) {
				messageRouter.addFilter((FilterStage) bean);
				return bean;
			}
			RTStage rtStage = aClass.getAnnotation(RTStage.class);
			String textQueryPath = rtStage.textQuery();
			if (textQueryPath.equals("")) textQueryPath = null;
			String prefix;
			String givenPrefix = "";
			if (aClass.isAnnotationPresent(RTMessageMapping.class)) {
				RTMessageMapping messageMapping = aClass.getAnnotation(RTMessageMapping.class);
				prefix = messageMapping.value();
				givenPrefix = messageMapping.value();
			} else {
				prefix = beanName.toLowerCase();
			}
			String defaultExceptionRedirect = "/";
			if (aClass.isAssignableFrom(RTExceptionRedirect.class)) {
				RTExceptionRedirect rtExceptionRedirect = aClass.getAnnotation(RTExceptionRedirect.class);
				defaultExceptionRedirect = rtExceptionRedirect.value();
			}
			for (Method method : aClass.getMethods()) {
				String exceptionRedirect = defaultExceptionRedirect;
				if (method.isAnnotationPresent(RTExceptionRedirect.class)) {
					RTExceptionRedirect rtExceptionRedirect = method.getAnnotation(RTExceptionRedirect.class);
					exceptionRedirect = rtExceptionRedirect.value();
				}
				if (method.isAnnotationPresent(RTMessageMapping.class)) {
					checkMethod(method);
					RTMessageMapping messageMapping = method.getAnnotation(RTMessageMapping.class);
					String path = resolve(givenPrefix, messageMapping.value());
					Method proxyMethod = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
					MappingMethodInfo mappingMethodInfo = new MappingMethodInfo(proxyMethod,
						method, bean, path, textQueryPath, exceptionRedirect);
					messageRouter.addMapping(mappingMethodInfo);
				}
			}
		}
		return bean;
	}
}
