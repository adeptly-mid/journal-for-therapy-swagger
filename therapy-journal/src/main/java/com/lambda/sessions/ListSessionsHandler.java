package com.lambda.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.model.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListSessionsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private static final String TABLE_NAME = "Session";
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      ScanRequest scanReq = ScanRequest.builder()
          .tableName(TABLE_NAME)
          .build();
      ScanResponse scanResponse = dynamoDb.scan(scanReq);

      List<Session> sessions = new ArrayList<>();
      for (Map<String, AttributeValue> item : scanResponse.items()) {
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
        // Optionally set clientId, startTime, endTime if present...
        sessions.add(session);
      }

      String responseBody = mapper.writeValueAsString(sessions);
      return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error listing sessions: " + e.getMessage());
      return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.getMessage());
    }
  }
}
