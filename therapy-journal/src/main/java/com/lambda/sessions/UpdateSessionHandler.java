package com.lambda.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.model.UpdateSessionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.util.HashMap;
import java.util.Map;

public class UpdateSessionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private static final String TABLE_NAME = "Session";
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      String sessionId = event.getPathParameters().get("sessionId");
      UpdateSessionRequest updateRequest = mapper.readValue(event.getBody(), UpdateSessionRequest.class);

      // Build the update expression and attribute maps dynamically based on non-null
      // fields
      StringBuilder updateExp = new StringBuilder("set ");
      Map<String, AttributeValue> attributeValues = new HashMap<>();
      Map<String, String> attributeNames = new HashMap<>();

      if (updateRequest.getTitle() != null) {
        updateExp.append("#t = :title, ");
        attributeValues.put(":title", AttributeValue.builder().s(updateRequest.getTitle()).build());
        attributeNames.put("#t", "title");
      }
      if (updateRequest.getPrivateNotes() != null) {
        updateExp.append("#pn = :privateNotes, ");
        attributeValues.put(":privateNotes", AttributeValue.builder().s(updateRequest.getPrivateNotes()).build());
        attributeNames.put("#pn", "privateNotes");
      }
      if (updateRequest.getSharedNotes() != null) {
        updateExp.append("#sn = :sharedNotes, ");
        attributeValues.put(":sharedNotes", AttributeValue.builder().s(updateRequest.getSharedNotes()).build());
        attributeNames.put("#sn", "sharedNotes");
      }
      if (updateRequest.getStatus() != null) {
        updateExp.append("#s = :status, ");
        attributeValues.put(":status", AttributeValue.builder().s(updateRequest.getStatus()).build());
        attributeNames.put("#s", "status");
      }
      // Remove trailing comma and space
      String updateExpression = updateExp.substring(0, updateExp.length() - 2);

      UpdateItemRequest updateReq = UpdateItemRequest.builder()
          .tableName(TABLE_NAME)
          .key(Map.of("sessionId", AttributeValue.builder().s(sessionId).build()))
          .updateExpression(updateExpression)
          .expressionAttributeValues(attributeValues)
          .expressionAttributeNames(attributeNames)
          .returnValues(ReturnValue.UPDATED_NEW)
          .build();
      dynamoDb.updateItem(updateReq);

      return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Session updated");
    } catch (Exception e) {
      context.getLogger().log("Error updating session: " + e.getMessage());
      return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.getMessage());
    }
  }
}
