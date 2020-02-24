package com.a6raywa1cher.rescheduletsuvk.component.router;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;

@Data
@AllArgsConstructor
public class MappingMethodInfo {
	private Method methodToCall;

	private Method infoProvider;

	private Object bean;

	private String mappingPath;

	private String defaultTextQueryParserPath;
}
