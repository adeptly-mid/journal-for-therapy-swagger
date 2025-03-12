package com.lambda.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Therapist;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListTherapistsForClientHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private static final String MAPPING_TABLE = System.getenv("MAPPING_TABLE");
  private static final String THERAPISTS_TABLE = System.getenv("THERAPISTS_TABLE");
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
      context.getLogger().log("Listing therapists for clientId: " + clientId);

      // Query mapping table for items with clientId.
      Map<String, AttributeValue> expressionValues = new HashMap<>();
      expressionValues.put(":clientId", AttributeValue.builder().s(clientId).build());

      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(MAPPING_TABLE)
          .keyConditionExpression("clientId = :clientId")
          .expressionAttributeValues(expressionValues)
          .build();

      List<Map<String, AttributeValue>> mappingItems = dynamoDbClient.query(queryRequest).items();

      List<Therapist> therapists = new ArrayList<>();
      // For each mapping, get therapist details from Therapists table.
      for (Map<String, AttributeValue> item : mappingItems) {
        String therapistId = item.get("therapistId").s();
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("therapistId", AttributeValue.builder().s(therapistId).build());
        GetItemRequest getItemRequest = GetItemRequest.builder()
            .tableName(THERAPISTS_TABLE)
            .key(key)
            .build();
        Map<String, AttributeValue> therapistItem = dynamoDbClient.getItem(getItemRequest).item();
        if (therapistItem != null && !therapistItem.isEmpty()) {
          String email = therapistItem.get("email").s();
          String specialization = therapistItem.get("specialization") != null ? therapistItem.get("specialization").s()
              : "";
          String name = therapistItem.get("name").s();
          therapists.add(new Therapist(therapistId, email, specialization, name));
        }
      }

      String responseBody = objectMapper.writeValueAsString(therapists);
      return createResponse(200, responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error listing therapists for client: " + e.getMessage());
      return createResponse(500, "{\"message\":\"Error listing therapists\"}");
    }
  }

  private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(statusCode);
    response.setBody(body);
    return response;
  }
}
