package com.lambda.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Client;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListClientsForTherapistHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final String MAPPING_TABLE = System.getenv("MAPPING_TABLE");
  private static final String CLIENTS_TABLE = System.getenv("CLIENTS_TABLE");
  private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    try {
      Map<String, String> pathParams = request.getPathParameters();
      if (pathParams == null || !pathParams.containsKey("therapistId")) {
        return createResponse(400, "{\"message\":\"Missing therapistId in path parameters\"}");
      }
      String therapistId = pathParams.get("therapistId");
      context.getLogger().log("Listing clients for therapistId: " + therapistId);

      // Query mapping table using the GSI "GSI_Mapping_TherapistId"
      Map<String, AttributeValue> expressionValues = new HashMap<>();
      expressionValues.put(":therapistId", AttributeValue.builder().s(therapistId).build());

      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(MAPPING_TABLE)
          .indexName("GSI_Mapping_TherapistId")
          .keyConditionExpression("therapistId = :therapistId")
          .expressionAttributeValues(expressionValues)
          .build();

      List<Map<String, AttributeValue>> mappingItems = dynamoDbClient.query(queryRequest).items();

      List<Client> clients = new ArrayList<>();
      // For each mapping, retrieve client details from Clients table.
      for (Map<String, AttributeValue> item : mappingItems) {
        String clientId = item.get("clientId").s();
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("clientId", AttributeValue.builder().s(clientId).build());
        GetItemRequest getItemRequest = GetItemRequest.builder()
            .tableName(CLIENTS_TABLE)
            .key(key)
            .build();
        Map<String, AttributeValue> clientItem = dynamoDbClient.getItem(getItemRequest).item();
        if (clientItem != null && !clientItem.isEmpty()) {
          String email = clientItem.get("email").s();
          String name = clientItem.get("name").s();
          clients.add(new Client(clientId, email, name));
        }
      }

      String responseBody = objectMapper.writeValueAsString(clients);
      return createResponse(200, responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error listing clients for therapist: " + e.getMessage());
      return createResponse(500, "{\"message\":\"Error listing clients\"}");
    }
  }

  private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(statusCode);
    response.setBody(body);
    return response;
  }
}
