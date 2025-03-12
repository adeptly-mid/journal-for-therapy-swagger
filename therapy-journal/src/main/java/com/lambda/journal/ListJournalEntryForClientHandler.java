package com.lambda.journal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.JournalEntry;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ListJournalEntryForClientHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String JOURNAL_TABLE = System.getenv("JOURNAL_TABLE");
  // Environment variable for access logs table; if not provided, default to
  // "JournalAccessLogs"
  private final String ACCESS_LOGS_TABLE = System.getenv("ACCESS_LOGS_TABLE") != null
      ? System.getenv("ACCESS_LOGS_TABLE")
      : "JournalAccessLogs";

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    // Set up headers for JSON and CORS.
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Access-Control-Allow-Origin", "*");

    // Get clientId from the path parameters.
    String clientId = event.getPathParameters().get("clientId");
    if (clientId == null) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withHeaders(headers)
          .withBody("{\"error\":\"Missing clientId in path parameters\"}");
    }

    // Inline logging: Log that the client is reading their journal entries.
    try {
      Map<String, AttributeValue> logItem = new HashMap<>();
      String timestamp = Instant.now().toString();
      String logId = UUID.randomUUID().toString();

      logItem.put("clientId", AttributeValue.builder().s(clientId).build());
      logItem.put("accessTimestamp", AttributeValue.builder().s(timestamp).build());
      logItem.put("logId", AttributeValue.builder().s(logId).build());
      // For a client reading their own journal, userId is the same as clientId.
      logItem.put("userId", AttributeValue.builder().s(clientId).build());
      logItem.put("accessType", AttributeValue.builder().s("clientReading").build());
      // We use "ALL" to denote that all entries are being read.
      logItem.put("journalEntryId", AttributeValue.builder().s("ALL").build());

      Map<String, AttributeValue> detailsMap = new HashMap<>();
      detailsMap.put("message", AttributeValue.builder().s("Client is reading their journal entries.").build());
      logItem.put("details", AttributeValue.builder().m(detailsMap).build());

      PutItemRequest putLogRequest = PutItemRequest.builder()
          .tableName(ACCESS_LOGS_TABLE)
          .item(logItem)
          .build();
      dynamoDb.putItem(putLogRequest);
      context.getLogger().log("Logged journal access event: " + logId);
    } catch (Exception e) {
      context.getLogger().log("Error logging journal access: " + e.getMessage());
      // Continue processing even if logging fails.
    }

    // Build the query to fetch journal entries using the LSI_Journal_Time index.
    Map<String, String> expressionNames = new HashMap<>();
    expressionNames.put("#cid", "clientId");

    Map<String, AttributeValue> expressionValues = new HashMap<>();
    expressionValues.put(":clientId", AttributeValue.builder().s(clientId).build());

    QueryRequest queryRequest = QueryRequest.builder()
        .tableName(JOURNAL_TABLE)
        .keyConditionExpression("#cid = :clientId")
        .expressionAttributeNames(expressionNames)
        .expressionAttributeValues(expressionValues)
        .indexName("LSI_Journal_Time")
        .build();

    QueryResponse queryResponse = dynamoDb.query(queryRequest);
    List<Map<String, AttributeValue>> items = queryResponse.items();

    // Convert each returned item into a JournalEntry POJO.
    List<JournalEntry> journalEntries = new ArrayList<>();
    for (Map<String, AttributeValue> item : items) {
      JournalEntry entry = new JournalEntry();
      entry.setJournalEntryId(item.get("journalEntryId") != null ? item.get("journalEntryId").s() : null);
      entry.setClientId(item.get("clientId") != null ? item.get("clientId").s() : null);
      entry.setTimeOfEmotion(item.get("timeOfEmotion") != null ? item.get("timeOfEmotion").s() : null);
      entry.setFeeling(item.get("feeling") != null ? item.get("feeling").s() : null);
      entry.setIntensity(item.get("intensity") != null ? Integer.parseInt(item.get("intensity").n()) : 0);
      entry.setCreatedAt(item.get("createdAt") != null ? item.get("createdAt").s() : null);
      entry.setContent(item.get("content") != null ? item.get("content").s() : null);
      journalEntries.add(entry);
    }

    // Convert the list of JournalEntry objects to JSON.
    String responseBody;
    try {
      responseBody = mapper.writeValueAsString(journalEntries);
    } catch (Exception e) {
      context.getLogger().log("Error converting journal entries to JSON: " + e.getMessage());
      responseBody = "{\"error\": \"Failed to convert response to JSON\"}";
    }

    return new APIGatewayProxyResponseEvent()
        .withStatusCode(200)
        .withHeaders(headers)
        .withBody(responseBody);
  }
}
