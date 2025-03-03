package com.lambda.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

public class RemoveClientFromTherapistHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final String MAPPING_TABLE = System.getenv("MAPPING_TABLE");
  private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      Map<String, String> pathParams = request.getPathParameters();
      if (pathParams == null || !pathParams.containsKey("therapistId") || !pathParams.containsKey("clientId")) {
        return createResponse(400, "{\"message\":\"Missing therapistId or clientId in path parameters\"}");
      }
      String therapistId = pathParams.get("therapistId");
      String clientId = pathParams.get("clientId");

      Map<String, AttributeValue> key = new HashMap<>();
      key.put("clientId", AttributeValue.builder().s(clientId).build());
      key.put("therapistId", AttributeValue.builder().s(therapistId).build());

      DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
          .tableName(MAPPING_TABLE)
          .key(key)
          .build();

      dynamoDbClient.deleteItem(deleteItemRequest);
      return createResponse(204, "");
    } catch (Exception e) {
      context.getLogger().log("Error removing client from therapist: " + e.getMessage());
      return createResponse(500, "{\"message\":\"Error removing client from therapist\"}");
    }
  }

  private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(statusCode);
    response.setBody(body);
    return response;
  }
}
