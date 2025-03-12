package com.lambda.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

public class RequestMappingHandler
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

      Map<String, AttributeValue> item = new HashMap<>();
      item.put("clientId", AttributeValue.builder().s(clientId).build());
      item.put("therapistId", AttributeValue.builder().s(therapistId).build());
      item.put("status", AttributeValue.builder().s("requested").build());

      PutItemRequest putItemRequest = PutItemRequest.builder()
          .tableName(MAPPING_TABLE)
          .item(item)
          .build();

      dynamoDbClient.putItem(putItemRequest);
      return createResponse(201, "{\"message\":\"Mapping request created\"}");
    } catch (Exception e) {
      context.getLogger().log("Error creating mapping request: " + e.getMessage());
      return createResponse(500, "{\"message\":\"Error creating mapping request\"}");
    }
  }

  private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(statusCode);
    response.setBody(body);
    return response;
  }
}
