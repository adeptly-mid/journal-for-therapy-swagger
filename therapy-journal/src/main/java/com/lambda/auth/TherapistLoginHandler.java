package com.lambda.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.common.Utils;
import com.model.AuthToken;
import com.model.LoginRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.Map;

public class TherapistLoginHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  private static final String THERAPISTS_TABLE = System.getenv("THERAPISTS_TABLE");
  private static final String GSI_THERAPISTS_BY_EMAIL = "GSI_THERAPISTS_ByEmail";
  private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      LoginRequest loginRequest = objectMapper.readValue(request.getBody(), LoginRequest.class);
      if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
        return createResponse(400, "{\"message\":\"Missing email or password\"}");
      }
      Map<String, String> expressionNames = new HashMap<>();
      expressionNames.put("#email", "email");

      Map<String, AttributeValue> expressionValues = new HashMap<>();
      expressionValues.put(":emailVal", AttributeValue.builder().s(loginRequest.getEmail()).build());

      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(THERAPISTS_TABLE).indexName(GSI_THERAPISTS_BY_EMAIL).keyConditionExpression("#email = :emailVal")
          .expressionAttributeNames(expressionNames)
          .expressionAttributeValues(expressionValues)
          .build();

      QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
      if (queryResponse.count() == 0) {
        return createResponse(401, "{\"message\":\"Invalid credentials\"}");
      }
      Map<String, AttributeValue> therapistItem = queryResponse.items().get(0);
      String storedPasswordHash = therapistItem.get("passwordHash").s();
      boolean passwordMatch = Utils.verifyPassword(loginRequest.getPassword(), storedPasswordHash);
      if (!passwordMatch) {
        return createResponse(401, "{\"message\":\"Invalid credentials\"}");
      }
      AuthToken authToken = new AuthToken("dummy-auth-token");
      return createResponse(200, objectMapper.writeValueAsString(authToken));
    } catch (Exception e) {
      context.getLogger().log("Error in login: " + e.getMessage());
      return createResponse(500, "{\"message\":\"Error during login\"}");
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
