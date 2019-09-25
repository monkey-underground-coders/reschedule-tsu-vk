package com.a6raywa1cher.rescheduletsuvk.config;

import com.a6raywa1cher.rescheduletsuvk.component.textquery.DialogFlowTextQueryProcessor;
import com.a6raywa1cher.rescheduletsuvk.component.textquery.EmptyTextQueryProcessor;
import com.a6raywa1cher.rescheduletsuvk.component.textquery.TextQueryExecutor;
import com.a6raywa1cher.rescheduletsuvk.component.textquery.TextQueryProcessor;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class TextQueryConfig {

	@Bean
	public TextQueryProcessor processor(AppConfigProperties properties, ResourceLoader resourceLoader,
	                                    TextQueryExecutor textQueryExecutor) throws Exception {
		if (properties.isActivateDialogFlow()) {
			Resource resource = resourceLoader.getResource("classpath:googlecredentials.json");
			GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());
			String projectId = ((ServiceAccountCredentials) credentials).getProjectId();
			SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
			SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
			return new DialogFlowTextQueryProcessor(projectId, sessionsSettings, textQueryExecutor);
		} else {
			return new EmptyTextQueryProcessor();
		}
	}
}
