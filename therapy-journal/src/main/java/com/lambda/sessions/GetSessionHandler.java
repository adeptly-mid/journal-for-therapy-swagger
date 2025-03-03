package com.lambda.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.model.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

public class GetSessionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private static final String TABLE_NAME = "Session";
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      String sessionId = event.getPathParameters().get("sessionId");

      Map<String, AttributeValue> key = new HashMap<>();
      key.put("sessionId", AttributeValue.builder().s(sessionId).build());

      GetItemRequest getItemRequest = GetItemRequest.builder()
          .tableName(TABLE_NAME)
          .key(key)
          .build();
      Map<String, AttributeValue> item = dynamoDb.getItem(getItemRequest).item();
      if (item == null || item.isEmpty()) {
        return new APIGatewayProxyResponseEvent().withStatusCode(404).withBody("Session not found");
      }

      Session session = new Session();
      session.setSessionId(item.get("sessionId").s());
      session.setTherapistId(item.get("therapistId").s());
      if (item.containsKey("title")) {
        session.setTitle(item.get("title").s());
      }
      if (item.containsKey("privateNotes")) {
        session.setPrivateNotes(item.get("privateNotes").s());
      }
      if (item.containsKey("sharedNotes")) {
        session.setSharedNotes(item.get("sharedNotes").s());
      }
      session.setStatus(item.get("status").s());
      session.setCreatedAt(item.get("createdAt").s());
      if (item.containsKey("clientId")) {
        session.setClientId(item.get("clientId").s());
      }
      // Optionally read startTime and endTime if applicable

      String responseBody = mapper.writeValueAsString(session);
      return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error retrieving session: " + e.getMessage());
      return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.getMessage());
    }
  }
}
