package com.a6raywa1cher.rescheduletsuvk.component.router;

import com.a6raywa1cher.rescheduletsuvk.filterstages.FilterStage;
import com.a6raywa1cher.rescheduletsuvk.stages.PrimaryStage;
import com.a6raywa1cher.rescheduletsuvk.stages.Stage;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class StageBeanPostProcessor implements BeanPostProcessor {
	private MessageRouter messageRouter;

	@Autowired
	public StageBeanPostProcessor(MessageRouter messageRouter) {
		this.messageRouter = messageRouter;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		Class<?> aClass = bean.getClass();
		if (PrimaryStage.class.isAssignableFrom(aClass)) {
			messageRouter.setPrimaryStage((PrimaryStage) bean);
		}
		if (Stage.class.isAssignableFrom(aClass)) {
			messageRouter.addStage((Stage) bean, beanName);
		}
		if (FilterStage.class.isAssignableFrom(aClass)) {
			messageRouter.addFilter((FilterStage) bean);
		}
		return bean;
	}
}
