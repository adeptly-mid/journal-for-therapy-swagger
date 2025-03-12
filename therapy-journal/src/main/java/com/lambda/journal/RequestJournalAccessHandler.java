package com.lambda.journal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.common.Utils;
import com.model.JournalAccessStatus;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;

public class RequestJournalAccessHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();

  // Adjust these environment variable names to match those set in your AWS Lambda
  // configuration
  private final String CLIENTS_TABLE = System.getenv("CLIENTS_TABLE");
  private final String THERAPISTS_TABLE = System.getenv("THERAPISTS_TABLE");
  private final String JOURNAL_ACCESS_STATUS_TABLE = System.getenv("JOURNAL_ACCESS_STATUS_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

    // Prepare a basic response structure with headers
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Access-Control-Allow-Origin", "*");

    // Extract path parameters
    String therapistId = event.getPathParameters().get("therapistId");
    String clientId = event.getPathParameters().get("clientId");

    // If either therapistId or clientId is missing, return 400
    if (therapistId == null || clientId == null) {
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withHeaders(headers)
          .withBody("{\"error\":\"Missing required path parameters\"}");
    }

    try {
      // 1. Check if client exists in the Clients table
      boolean clientExists = Utils.itemExistsInTable(dynamoDb, CLIENTS_TABLE, "clientId", clientId);
      if (!clientExists) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withHeaders(headers)
            .withBody("{\"error\":\"Client not found\"}");
      }

      // 2. Check if therapist exists in the Therapists table
      boolean therapistExists = Utils.itemExistsInTable(dynamoDb, THERAPISTS_TABLE, "therapistId", therapistId);
      if (!therapistExists) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withHeaders(headers)
            .withBody("{\"error\":\"Therapist not found\"}");
      }

      // 3. Create a "requested" access status
      JournalAccessStatus accessStatus = new JournalAccessStatus(clientId, therapistId, "requested");

      // 4. Put the item in the JournalAccessStatus table
      Map<String, AttributeValue> itemValues = new HashMap<>();
      itemValues.put("clientId", AttributeValue.builder().s(accessStatus.getClientId()).build());
      itemValues.put("therapistId", AttributeValue.builder().s(accessStatus.getTherapistId()).build());
      itemValues.put("access", AttributeValue.builder().s(accessStatus.getAccess()).build());

      PutItemRequest putItemRequest = PutItemRequest.builder()
          .tableName(JOURNAL_ACCESS_STATUS_TABLE)
          .item(itemValues)
          .build();

      dynamoDb.putItem(putItemRequest);

      // 5. Return a success response (HTTP 201 Created)
      String responseBody = "{\"message\":\"Request for journal access created\"}";
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(201)
          .withHeaders(headers)
          .withBody(responseBody);

    } catch (ResourceNotFoundException rnfe) {
      // This exception can occur if the table doesn't exist
      context.getLogger().log("Table not found: " + rnfe.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withHeaders(headers)
          .withBody("{\"error\":\"Internal Server Error - Table not found\"}");
    } catch (Exception e) {
      // Catch-all for other exceptions
      context.getLogger().log("Error: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withHeaders(headers)
          .withBody("{\"error\":\"Internal Server Error\"}");
    }
  }
}
