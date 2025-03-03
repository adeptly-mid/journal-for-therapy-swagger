package com.lambda.therapist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.CreateSessionRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateSessionForTherapistHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String sessionTable = System.getenv("SESSION_TABLE");
  private final String therapistsTable = System.getenv("THERAPISTS_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      Map<String, String> pathParams = event.getPathParameters();
      if (pathParams == null || !pathParams.containsKey("therapistId")) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody("Missing therapistId in path parameters");
      }
      String therapistId = pathParams.get("therapistId");

      // Verify that the therapist exists
      Map<String, AttributeValue> therapistKey = new HashMap<>();
      therapistKey.put("therapistId", AttributeValue.builder().s(therapistId).build());
      GetItemRequest getTherapistRequest = GetItemRequest.builder()
          .tableName(therapistsTable)
          .key(therapistKey)
          .build();
      Map<String, AttributeValue> therapistItem = dynamoDb.getItem(getTherapistRequest).item();
      if (therapistItem == null || therapistItem.isEmpty()) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withBody("Therapist not found");
      }

      // Parse request body for session details
      CreateSessionRequest createSessionRequest = mapper.readValue(event.getBody(), CreateSessionRequest.class);

      // Generate a new session ID
      String sessionId = UUID.randomUUID().toString();

      // Build the session item to put into DynamoDB
      Map<String, AttributeValue> item = new HashMap<>();
      item.put("sessionId", AttributeValue.builder().s(sessionId).build());
      item.put("therapistId", AttributeValue.builder().s(therapistId).build());
      if (createSessionRequest.getClientId() != null) {
        item.put("clientId", AttributeValue.builder().s(createSessionRequest.getClientId()).build());
      }
      if (createSessionRequest.getTitle() != null) {
        item.put("title", AttributeValue.builder().s(createSessionRequest.getTitle()).build());
      }
      if (createSessionRequest.getPrivateNotes() != null) {
        item.put("privateNotes", AttributeValue.builder().s(createSessionRequest.getPrivateNotes()).build());
      }
      if (createSessionRequest.getSharedNotes() != null) {
        item.put("sharedNotes", AttributeValue.builder().s(createSessionRequest.getSharedNotes()).build());
      }
      // Use provided status if set; otherwise default to "pending"
      String status = (createSessionRequest.getStatus() != null) ? createSessionRequest.getStatus() : "pending";
      item.put("status", AttributeValue.builder().s(status).build());
      if (createSessionRequest.getStartTime() != null) {
        item.put("startTime", AttributeValue.builder().s(createSessionRequest.getStartTime()).build());
      }
      if (createSessionRequest.getEndTime() != null) {
        item.put("endTime", AttributeValue.builder().s(createSessionRequest.getEndTime()).build());
      }

      // Put the new session into the Session table
      PutItemRequest putItemRequest = PutItemRequest.builder()
          .tableName(sessionTable)
          .item(item)
          .build();
      dynamoDb.putItem(putItemRequest);

      // Return a JSON response with the created session ID
      String responseBody = "{\"sessionId\":\"" + sessionId + "\"}";
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(201)
          .withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error creating session: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody("Internal server error");
    }
  }
}
