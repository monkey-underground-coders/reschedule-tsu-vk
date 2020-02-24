package com.a6raywa1cher.rescheduletsuvk.component.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface RTContainerEntity {
	String value() default "";

	boolean nullable() default false;
}
