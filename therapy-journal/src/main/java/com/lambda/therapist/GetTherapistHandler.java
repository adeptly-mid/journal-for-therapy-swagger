package com.lambda.therapist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Therapist;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.HashMap;
import java.util.Map;

public class GetTherapistHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final String THERAPISTS_TABLE = System.getenv("THERAPISTS_TABLE");
  private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      Map<String, String> pathParameters = request.getPathParameters();
      if (pathParameters == null || !pathParameters.containsKey("therapistId")) {
        return createResponse(400, "{\"message\":\"Missing therapistId in path parameters\"}");
      }
      String therapistId = pathParameters.get("therapistId");
      context.getLogger().log("Retrieving therapist with therapistId: " + therapistId);

      // Build the key to query the Therapists table.
      Map<String, AttributeValue> key = new HashMap<>();
      key.put("therapistId", AttributeValue.builder().s(therapistId).build());

      GetItemRequest getItemRequest = GetItemRequest.builder()
          .tableName(THERAPISTS_TABLE)
          .key(key)
          .build();

      Map<String, AttributeValue> item = dynamoDbClient.getItem(getItemRequest).item();

      if (item == null || item.isEmpty()) {
        return createResponse(404, "{\"message\":\"Therapist not found\"}");
      }

      // Create a Therapist POJO using data from DynamoDB.
      String email = item.get("email").s();
      String specialization = item.get("specialization") != null ? item.get("specialization").s() : "";
      String name = item.get("name").s();
      Therapist therapist = new Therapist(therapistId, email, specialization, name);

      String responseBody = objectMapper.writeValueAsString(therapist);
      return createResponse(200, responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error in getTherapist: " + e.getMessage());
      return createResponse(500, "{\"message\":\"Error retrieving therapist\"}");
    }
  }

  // Helper method to create API Gateway responses.
  private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(statusCode);
    response.setBody(body);
    return response;
  }
}
