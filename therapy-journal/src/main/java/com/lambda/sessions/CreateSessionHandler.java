package com.lambda.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.CreateSessionRequest;
import com.model.Session;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateSessionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private static final String TABLE_NAME = "Session";
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      // Parse the incoming request
      CreateSessionRequest createRequest = mapper.readValue(event.getBody(), CreateSessionRequest.class);

      // Validate required fields: therapistId and title
      if (createRequest.getTherapistId() == null || createRequest.getTherapistId().trim().isEmpty() ||
          createRequest.getTitle() == null || createRequest.getTitle().trim().isEmpty()) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody("therapistId and title are required fields");
      }

      // Create a new Session instance
      String sessionId = UUID.randomUUID().toString();
      Session session = new Session();
      session.setSessionId(sessionId);
      session.setTherapistId(createRequest.getTherapistId());
      session.setTitle(createRequest.getTitle());
      // Optional fields
      session.setClientId(createRequest.getClientId());
      session.setPrivateNotes(createRequest.getPrivateNotes());
      session.setSharedNotes(createRequest.getSharedNotes());
      session.setStatus(createRequest.getStatus() != null ? createRequest.getStatus() : "open");
      session.setCreatedAt(Instant.now().toString());

      // Prepare the item for DynamoDB
      Map<String, AttributeValue> item = new HashMap<>();
      item.put("sessionId", AttributeValue.builder().s(session.getSessionId()).build());
      item.put("therapistId", AttributeValue.builder().s(session.getTherapistId()).build());
      item.put("title", AttributeValue.builder().s(session.getTitle()).build());
      if (session.getClientId() != null) {
        item.put("clientId", AttributeValue.builder().s(session.getClientId()).build());
      }
      if (session.getPrivateNotes() != null) {
        item.put("privateNotes", AttributeValue.builder().s(session.getPrivateNotes()).build());
      }
      if (session.getSharedNotes() != null) {
        item.put("sharedNotes", AttributeValue.builder().s(session.getSharedNotes()).build());
      }
      item.put("status", AttributeValue.builder().s(session.getStatus()).build());
      item.put("createdAt", AttributeValue.builder().s(session.getCreatedAt()).build());

      // Persist the new session in DynamoDB
      PutItemRequest putReq = PutItemRequest.builder()
          .tableName(TABLE_NAME)
          .item(item)
          .build();
      dynamoDb.putItem(putReq);

      // Return the created session with 201 status code
      String responseBody = mapper.writeValueAsString(session);
      return new APIGatewayProxyResponseEvent().withStatusCode(201).withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error creating session: " + e.getMessage());
      return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.getMessage());
    }
  }
}
