package com.lambda.therapist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.UpdateSessionRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.HashMap;
import java.util.Map;

public class UpdateSessionForTherapistHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String sessionTable = System.getenv("SESSION_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      // Validate path parameters
      Map<String, String> pathParams = event.getPathParameters();
      if (pathParams == null || !pathParams.containsKey("therapistId") || !pathParams.containsKey("sessionId")) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody("Missing therapistId or sessionId in path parameters");
      }
      String therapistId = pathParams.get("therapistId");
      String sessionId = pathParams.get("sessionId");

      // Parse the request body into UpdateSessionRequest
      UpdateSessionRequest updateRequest = mapper.readValue(event.getBody(), UpdateSessionRequest.class);

      // Build update expression dynamically based on provided attributes.
      StringBuilder updateExpression = new StringBuilder("set ");
      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      Map<String, String> expressionAttributeNames = new HashMap<>();
      boolean first = true;

      if (updateRequest.getStatus() != null) {
        updateExpression.append("#s = :status");
        expressionAttributeNames.put("#s", "status");
        expressionAttributeValues.put(":status", AttributeValue.builder().s(updateRequest.getStatus()).build());
        first = false;
      }
      if (updateRequest.getTitle() != null) {
        if (!first) {
          updateExpression.append(", ");
        }
        updateExpression.append("#t = :title");
        expressionAttributeNames.put("#t", "title");
        expressionAttributeValues.put(":title", AttributeValue.builder().s(updateRequest.getTitle()).build());
        first = false;
      }
      if (updateRequest.getPrivateNotes() != null) {
        if (!first) {
          updateExpression.append(", ");
        }
        updateExpression.append("#pn = :privateNotes");
        expressionAttributeNames.put("#pn", "privateNotes");
        expressionAttributeValues.put(":privateNotes",
            AttributeValue.builder().s(updateRequest.getPrivateNotes()).build());
        first = false;
      }
      if (updateRequest.getSharedNotes() != null) {
        if (!first) {
          updateExpression.append(", ");
        }
        updateExpression.append("#sn = :sharedNotes");
        expressionAttributeNames.put("#sn", "sharedNotes");
        expressionAttributeValues.put(":sharedNotes",
            AttributeValue.builder().s(updateRequest.getSharedNotes()).build());
        first = false;
      }
      if (updateRequest.getStartTime() != null) {
        if (!first) {
          updateExpression.append(", ");
        }
        updateExpression.append("#st = :startTime");
        expressionAttributeNames.put("#st", "startTime");
        expressionAttributeValues.put(":startTime", AttributeValue.builder().s(updateRequest.getStartTime()).build());
        first = false;
      }
      if (updateRequest.getEndTime() != null) {
        if (!first) {
          updateExpression.append(", ");
        }
        updateExpression.append("#et = :endTime");
        expressionAttributeNames.put("#et", "endTime");
        expressionAttributeValues.put(":endTime", AttributeValue.builder().s(updateRequest.getEndTime()).build());
        first = false;
      }
      if (updateExpression.toString().equals("set ")) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody("No attributes provided to update");
      }

      // Use a condition expression to ensure that the session belongs to the
      // specified therapist.
      String conditionExpression = "therapistId = :therapistIdCondition";
      expressionAttributeValues.put(":therapistIdCondition", AttributeValue.builder().s(therapistId).build());

      // Prepare the key for the session item.
      Map<String, AttributeValue> key = new HashMap<>();
      key.put("sessionId", AttributeValue.builder().s(sessionId).build());

      UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
          .tableName(sessionTable)
          .key(key)
          .updateExpression(updateExpression.toString())
          .expressionAttributeNames(expressionAttributeNames)
          .expressionAttributeValues(expressionAttributeValues)
          .conditionExpression(conditionExpression)
          .returnValues(ReturnValue.UPDATED_NEW)
          .build();

      dynamoDb.updateItem(updateItemRequest);

      return new APIGatewayProxyResponseEvent()
          .withStatusCode(200)
          .withBody("Session updated successfully");
    } catch (ConditionalCheckFailedException e) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(404)
          .withBody("Session not found or does not belong to the specified therapist");
    } catch (Exception e) {
      context.getLogger().log("Error updating session: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody("Internal server error");
    }
  }
}
