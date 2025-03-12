
package com.lambda.client;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Client;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.HashMap;
import java.util.Map;

public class GetClientHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  // Read the DynamoDB table name from the environment variable.
  private static final String CLIENTS_TABLE = System.getenv("CLIENTS_TABLE");
  private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      // Extract clientId from the path parameters.
      Map<String, String> pathParameters = request.getPathParameters();
      if (pathParameters == null || !pathParameters.containsKey("clientId")) {
        return createResponse(400, "{\"message\":\"Missing clientId in path parameters\"}");
      }
      String clientId = pathParameters.get("clientId");
      context.getLogger().log("Retrieving client with clientId: " + clientId);

      // Build the key to query the Clients table.
      Map<String, AttributeValue> key = new HashMap<>();
      key.put("clientId", AttributeValue.builder().s(clientId).build());

      // Create the GetItem request.
      GetItemRequest getItemRequest = GetItemRequest.builder()
          .tableName(CLIENTS_TABLE)
          .key(key)
          .build();

      // Query DynamoDB for the client item.
      Map<String, AttributeValue> item = dynamoDbClient.getItem(getItemRequest).item();

      if (item == null || item.isEmpty()) {
        return createResponse(404, "{\"message\":\"Client not found\"}");
      }

      // Build the Client POJO from the DynamoDB item.
      String email = item.get("email").s();
      String name = item.get("name").s();
      Client client = new Client(clientId, email, name);

      // Return the client details as JSON.
      String responseBody = objectMapper.writeValueAsString(client);
      return createResponse(200, responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error in getClient: " + e.getMessage());
      return createResponse(500, "{\"message\":\"Error retrieving client\"}");
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
