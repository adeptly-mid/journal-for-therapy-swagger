package com.lambda.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.MapRequest; // Import the MapRequest POJO
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;

public class MapTherapistToClientHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final String MAPPING_TABLE = System.getenv("MAPPING_TABLE");
  private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      Map<String, String> pathParams = request.getPathParameters();
      if (pathParams == null || !pathParams.containsKey("clientId")) {
        return createResponse(400, "{\"message\":\"Missing clientId in path parameters\"}");
      }
      String clientId = pathParams.get("clientId");

      String body = request.getBody();
      if (body == null || body.trim().isEmpty()) {
        return createResponse(400, "{\"message\":\"Request body is empty\"}");
      }

      MapRequest mapRequest = objectMapper.readValue(body, MapRequest.class);
      if (mapRequest.getTherapistId() == null || mapRequest.getTherapistId().trim().isEmpty()) {
        return createResponse(400, "{\"message\":\"Missing therapistId in request body\"}");
      }

      // Prepare the mapping item.
      Map<String, AttributeValue> item = new HashMap<>();
      item.put("clientId", AttributeValue.builder().s(clientId).build());
      item.put("therapistId", AttributeValue.builder().s(mapRequest.getTherapistId()).build());
      item.put("status", AttributeValue.builder().s("mapped").build());

      PutItemRequest putItemRequest = PutItemRequest.builder()
          .tableName(MAPPING_TABLE)
          .item(item)
          .build();

      dynamoDbClient.putItem(putItemRequest);
      return createResponse(201, "{\"message\":\"Therapist mapped to client\"}");
    } catch (Exception e) {
      context.getLogger().log("Error mapping therapist to client: " + e.getMessage());
      return createResponse(500, "{\"message\":\"Error mapping therapist to client\"}");
    }
  }

  private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(statusCode);
    response.setBody(body);
    return response;
  }
}
