package com.lambda.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.model.AppointmentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.HashMap;
import java.util.Map;

public class AppointmentRequestHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private static final String TABLE_NAME = "Session";
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      String sessionId = event.getPathParameters().get("sessionId");
      AppointmentRequest appointmentRequest = mapper.readValue(event.getBody(), AppointmentRequest.class);

      // Update the session item: set clientId and enforce the status to "requested"
      String updateExpression = "set clientId = :clientId, #s = :status";
      Map<String, AttributeValue> attributeValues = new HashMap<>();
      attributeValues.put(":clientId", AttributeValue.builder().s(appointmentRequest.getClientId()).build());
      // Always set status to "requested"
      attributeValues.put(":status", AttributeValue.builder().s("requested").build());

      Map<String, String> attributeNames = new HashMap<>();
      attributeNames.put("#s", "status");

      // Add a condition to check if the session exists
      UpdateItemRequest updateReq = UpdateItemRequest.builder()
          .tableName(TABLE_NAME)
          .key(Map.of("sessionId", AttributeValue.builder().s(sessionId).build()))
          .updateExpression(updateExpression)
          .expressionAttributeValues(attributeValues)
          .expressionAttributeNames(attributeNames)
          .conditionExpression("attribute_exists(sessionId)")
          .returnValues(ReturnValue.UPDATED_NEW)
          .build();

      dynamoDb.updateItem(updateReq);

      return new APIGatewayProxyResponseEvent()
          .withStatusCode(201)
          .withBody("Appointment request created");
    } catch (ConditionalCheckFailedException e) {
      context.getLogger().log("Session not found: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(404)
          .withBody("Session not found");
    } catch (Exception e) {
      context.getLogger().log("Error processing appointment request: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody(e.getMessage());
    }
  }
}
