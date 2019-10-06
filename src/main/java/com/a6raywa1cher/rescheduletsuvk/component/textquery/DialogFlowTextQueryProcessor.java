package com.a6raywa1cher.rescheduletsuvk.component.textquery;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;
import com.a6raywa1cher.rescheduletsuvk.models.StudentUser;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.google.cloud.dialogflow.v2.*;
import com.google.protobuf.Value;
import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DialogFlowTextQueryProcessor implements TextQueryProcessor {
	private static final Logger log = LoggerFactory.getLogger(DialogFlowTextQueryProcessor.class);
	private static final String GET_PAIRS = "Пары на день";
	private String projectId;
	private SessionsSettings sessionsSettings;
	private TextQueryExecutor textQueryExecutor;

	public DialogFlowTextQueryProcessor(String projectId, SessionsSettings sessionsSettings, TextQueryExecutor textQueryExecutor) {
		this.projectId = projectId;
		this.sessionsSettings = sessionsSettings;
		this.textQueryExecutor = textQueryExecutor;
	}

	private QueryResult detectIntentTexts(
			String text,
			String sessionId,
			String languageCode) throws Exception {
		// Instantiates a client
		try (SessionsClient sessionsClient = SessionsClient.create(sessionsSettings)) {
			// Set the session name using the sessionId (UUID) and projectID (my-project-id)
			SessionName session = SessionName.of(projectId, sessionId);
			log.debug("Session Path: " + session.toString());

			// Set the text (hello) and language code (en-US) for the query
			TextInput.Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode(languageCode);

			// Build the query with the TextInput
			QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

			// Performs the detect intent request
			DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

			// Display the query result
			QueryResult queryResult = response.getQueryResult();

			log.debug("Query Text: '{}'", queryResult.getQueryText());
			log.debug("Detected Intent: {} (confidence: {})",
					queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
			log.debug("Fulfillment Text: '{}'", queryResult.getFulfillmentText());

			return queryResult;
		}
	}

	private void getPairs(UserInfo userInfo, ExtendedMessage extendedMessage, QueryResult queryResult) {
		if (userInfo instanceof StudentUser) {
			Map<String, Value> map = queryResult.getParameters().getFieldsMap();
			String group = map.containsKey("group_name") && map.get("group_name").getNumberValue() != 0d ? Integer.toString((int) map.get("group_name").getNumberValue())
					: ((StudentUser) userInfo).getGroupId();
			LocalDateTime localDateTime = map.containsKey("date") ? LocalDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(
					map.get("date").getStringValue())) : LocalDateTime.now();
			log.debug("group: {}, ldt:{}", group, localDateTime.toString());
			textQueryExecutor.getPair(userInfo, extendedMessage, group, localDateTime.toLocalDate());
		}
	}

	@Override
	public boolean process(UserInfo userInfo, ExtendedMessage extendedMessage) {
		try {
			if (extendedMessage.getBody() == null || extendedMessage.getBody().isBlank() ||
					extendedMessage.getBody().length() > 250) return false;
			QueryResult queryResult = detectIntentTexts(extendedMessage.getBody(),
					Integer.toString(extendedMessage.getUserId()), "ru-RU");
			switch (queryResult.getIntent().getDisplayName()) {
				case GET_PAIRS:
					getPairs(userInfo, extendedMessage, queryResult);
					return true;
				default:
					return false;
			}
		} catch (Exception e) {
			log.error("DialogFlow error", e);
			Sentry.capture(e);
			return false;
		}
	}
}
