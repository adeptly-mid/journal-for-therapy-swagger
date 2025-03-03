package com.lambda.journal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResolveTherapistAccessRequestHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();

  // Environment variables (configured in your Lambda settings)
  private final String ACCESS_LOGS_TABLE = System.getenv("ACCESS_LOGS_TABLE");
  private final String JOURNAL_ACCESS_STATUS_TABLE = System.getenv("JOURNAL_ACCESS_STATUS_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

    // Prepare basic headers for JSON and CORS
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Access-Control-Allow-Origin", "*");

    try {
      // Read path parameters: clientId, therapistId
      String clientId = event.getPathParameters().get("clientId");
      String therapistId = event.getPathParameters().get("therapistId");

      if (clientId == null || therapistId == null) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withHeaders(headers)
            .withBody("{\"error\": \"Missing path parameters\"}");
      }

      // Parse request body for the "approve" field
      JsonNode json = mapper.readTree(event.getBody());
      if (!json.has("approve")) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withHeaders(headers)
            .withBody("{\"error\": \"Missing 'approve' field in JSON body\"}");
      }
      boolean approved = json.get("approve").asBoolean();

      // 1. Update or create the record in the JournalAccessStatus table
      // We'll store "allowed" if approved, or "denied" if not.
      String accessStatusValue = approved ? "allowed" : "denied";

      Map<String, AttributeValue> keyMap = new HashMap<>();
      keyMap.put("clientId", AttributeValue.builder().s(clientId).build());
      keyMap.put("therapistId", AttributeValue.builder().s(therapistId).build());

      Map<String, String> expressionAttributeNames = new HashMap<>();
      expressionAttributeNames.put("#A", "access"); // #A references the "access" field

      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      expressionAttributeValues.put(":val", AttributeValue.builder().s(accessStatusValue).build());

      UpdateItemRequest updateRequest = UpdateItemRequest.builder()
          .tableName(JOURNAL_ACCESS_STATUS_TABLE)
          .key(keyMap)
          .updateExpression("SET #A = :val")
          .expressionAttributeNames(expressionAttributeNames)
          .expressionAttributeValues(expressionAttributeValues)
          // If no item exists yet, this will create one (upsert behavior)
          .build();

      dynamoDb.updateItem(updateRequest);

      // 2. Log this access resolution in the AccessLogs table
      // (same approach as the original code)
      Map<String, AttributeValue> logItem = new HashMap<>();
      logItem.put("clientId", AttributeValue.builder().s(clientId).build());
      logItem.put("accessTimestamp", AttributeValue.builder().s(Instant.now().toString()).build());
      logItem.put("logId", AttributeValue.builder().s(UUID.randomUUID().toString()).build());
      logItem.put("userId", AttributeValue.builder().s(therapistId).build());
      // We'll mark it as APPROVED or REJECTED
      logItem.put("accessType", AttributeValue.builder().s(approved ? "APPROVED" : "REJECTED").build());
      logItem.put("details", AttributeValue.builder().m(new HashMap<>()).build());

      PutItemRequest putLog = PutItemRequest.builder()
          .tableName(ACCESS_LOGS_TABLE)
          .item(logItem)
          .build();
      dynamoDb.putItem(putLog);

      // Return a successful response
      String responseBody = String.format("{\"message\": \"Therapist access %s.\"}",
          approved ? "approved" : "rejected");

      return new APIGatewayProxyResponseEvent()
          .withStatusCode(200)
          .withHeaders(headers)
          .withBody(responseBody);

    } catch (Exception e) {
      context.getLogger().log("Error: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withHeaders(headers)
          .withBody("{\"error\": \"Internal Server Error\"}");
    }
  }
}
