package com.lambda.therapist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

public class GetSessionForTherapistHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String sessionTable = System.getenv("SESSION_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      Map<String, String> pathParams = event.getPathParameters();
      if (pathParams == null || !pathParams.containsKey("therapistId") || !pathParams.containsKey("sessionId")) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody("Missing therapistId or sessionId in path parameters");
      }
      String therapistId = pathParams.get("therapistId");
      String sessionId = pathParams.get("sessionId");

      // Retrieve the session item from the Session table
      Map<String, AttributeValue> key = new HashMap<>();
      key.put("sessionId", AttributeValue.builder().s(sessionId).build());
      GetItemRequest getItemRequest = GetItemRequest.builder()
          .tableName(sessionTable)
          .key(key)
          .build();
      Map<String, AttributeValue> sessionItem = dynamoDb.getItem(getItemRequest).item();

      if (sessionItem == null || sessionItem.isEmpty()) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withBody("Session not found");
      }

      // Verify that the session's therapistId matches the provided therapistId
      String itemTherapistId = sessionItem.get("therapistId").s();
      if (!therapistId.equals(itemTherapistId)) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withBody("Session does not belong to the specified therapist");
      }

      // Convert the session item to a JSON string and return it
      String responseBody = mapper.writeValueAsString(sessionItem);
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(200)
          .withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error getting session: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody("Internal server error");
    }
  }
}
