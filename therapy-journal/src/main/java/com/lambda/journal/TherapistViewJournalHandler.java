package com.lambda.journal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.JournalEntry;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TherapistViewJournalHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();

  // Table with the actual journal entries
  private final String JOURNAL_TABLE = System.getenv("JOURNAL_TABLE");
  // Table that stores (clientId, therapistId, access) e.g. "allowed", "denied",
  // etc.
  private final String JOURNAL_ACCESS_STATUS_TABLE = System.getenv("JOURNAL_ACCESS_STATUS_TABLE");
  // JournalAccessLogs table name (using environment variable is optional; here we
  // use a fixed name)
  private final String JOURNAL_ACCESS_LOGS_TABLE = "JournalAccessLogs";

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

    // Create headers for CORS and content type.
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Access-Control-Allow-Origin", "*");

    // Retrieve path parameters.
    String clientId = event.getPathParameters().get("clientId");
    String therapistId = event.getPathParameters().get("therapistId");

    if (therapistId == null || clientId == null) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withHeaders(headers)
          .withBody("{\"error\":\"Missing required path parameters\"}");
    }

    // 1. Check if therapist has "allowed" access to the client's journal.
    if (!therapistHasAllowedAccess(clientId, therapistId, context)) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(403)
          .withHeaders(headers)
          .withBody("{\"error\":\"Therapist does not have permission to view this client's journal.\"}");
    }

    // Inline logging: Create a log entry in the JournalAccessLogs table.
    try {
      Map<String, AttributeValue> logItem = new HashMap<>();
      String timestamp = Instant.now().toString();
      String logId = UUID.randomUUID().toString();
      logItem.put("clientId", AttributeValue.builder().s(clientId).build());
      logItem.put("accessTimestamp", AttributeValue.builder().s(timestamp).build());
      logItem.put("logId", AttributeValue.builder().s(logId).build());
      // Store therapist's ID under "userId"
      logItem.put("userId", AttributeValue.builder().s(therapistId).build());
      logItem.put("accessType", AttributeValue.builder().s("reading").build());
      // Using "ALL" to denote that all entries were viewed.
      logItem.put("journalEntryId", AttributeValue.builder().s("ALL").build());
      Map<String, AttributeValue> detailsMap = new HashMap<>();
      detailsMap.put("message",
          AttributeValue.builder().s("Therapist is reading the client's journal entries.").build());
      logItem.put("details", AttributeValue.builder().m(detailsMap).build());

      PutItemRequest putLogRequest = PutItemRequest.builder()
          .tableName(JOURNAL_ACCESS_LOGS_TABLE)
          .item(logItem)
          .build();
      dynamoDb.putItem(putLogRequest);
      context.getLogger().log("Logged journal access event: " + logId);
    } catch (Exception e) {
      context.getLogger().log("Error logging journal access: " + e.getMessage());
      // Continue processing even if logging fails.
    }

    context.getLogger().log("Therapist " + therapistId + " is accessing journal entries for client " + clientId);

    // 2. Query the Journal table using the clientId and the LSI_Journal_Time index.
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

  /**
   * Checks if the therapist has allowed access to the client's journal.
   */
  private boolean therapistHasAllowedAccess(String clientId, String therapistId, Context context) {
    try {
      Map<String, AttributeValue> keyMap = new HashMap<>();
      keyMap.put("clientId", AttributeValue.builder().s(clientId).build());
      keyMap.put("therapistId", AttributeValue.builder().s(therapistId).build());

      GetItemRequest getItemRequest = GetItemRequest.builder()
          .tableName(JOURNAL_ACCESS_STATUS_TABLE)
          .key(keyMap)
          .build();

      GetItemResponse getItemResponse = dynamoDb.getItem(getItemRequest);
      if (!getItemResponse.hasItem()) {
        return false;
      }

      Map<String, AttributeValue> item = getItemResponse.item();
      String accessStatus = item.containsKey("access") ? item.get("access").s() : "";
      return "allowed".equalsIgnoreCase(accessStatus);
    } catch (Exception e) {
      context.getLogger().log("Error checking access status: " + e.getMessage());
      return false;
    }
  }
}
