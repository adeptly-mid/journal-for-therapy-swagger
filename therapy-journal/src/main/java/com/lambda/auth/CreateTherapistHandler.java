package com.lambda.auth;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.common.Utils;
import com.model.CreateTherapistRequest;
import com.model.Therapist;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.Map;

public class CreateTherapistHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
  // Fixed typo in the environment variable name
  private static final String THERAPISTS_TABLE = System.getenv("THERAPISTS_TABLE");
  private static final String GSI_THERAPISTS_BY_EMAIL = "GSI_Therapists_ByEmail";
  private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
    context.getLogger().log("Handler started. Table name: " + THERAPISTS_TABLE);

    try {
      String body = request.getBody();
      context.getLogger().log("Received request body: " + body);

      if (body == null || body.trim().isEmpty()) {
        return createResponse(400, "{\"message\":\"Request body is empty\"}");
      }

      CreateTherapistRequest therapistRequest = objectMapper.readValue(body, CreateTherapistRequest.class);

      // Validate required fields (check for null or empty strings)
      if (therapistRequest.getEmail() == null || therapistRequest.getEmail().trim().isEmpty() ||
          therapistRequest.getPassword() == null || therapistRequest.getPassword().trim().isEmpty() ||
          therapistRequest.getName() == null || therapistRequest.getName().trim().isEmpty()
          || therapistRequest.getSpecialization() == null || therapistRequest.getSpecialization().trim().isEmpty()) {
        return createResponse(400, "{\"message\":\"Missing required fields: email, password,name or specialization\"}");
      }

      // First check if the email already exists
      Map<String, String> expressionNames = new HashMap<>();
      expressionNames.put("#email", "email");

      Map<String, AttributeValue> expressionValues = new HashMap<>();
      expressionValues.put(":emailVal", AttributeValue.builder().s(therapistRequest.getEmail()).build());

      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(THERAPISTS_TABLE)
          .indexName(GSI_THERAPISTS_BY_EMAIL)
          .keyConditionExpression("#email = :emailVal")
          .expressionAttributeNames(expressionNames)
          .expressionAttributeValues(expressionValues)
          .build();

      QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
      if (queryResponse.count() > 0) {
        return createResponse(400, "{\"message\":\"Email already exists\"}");
      }

      String therapistId = Utils.generateClientId();
      String passwordHash = Utils.hashPassword(therapistRequest.getPassword());

      context.getLogger().log("Generated therapistId: " + therapistId);

      Map<String, AttributeValue> item = new HashMap<>();
      // Fixed field name to match table schema
      item.put("therapistId", AttributeValue.builder().s(therapistId).build());
      item.put("email", AttributeValue.builder().s(therapistRequest.getEmail()).build());
      item.put("name", AttributeValue.builder().s(therapistRequest.getName()).build());
      item.put("passwordHash", AttributeValue.builder().s(passwordHash).build());
      item.put("specialization", AttributeValue.builder().s(therapistRequest.getSpecialization()).build());

      PutItemRequest putItemRequest = PutItemRequest.builder()
          .tableName(THERAPISTS_TABLE)
          .item(item)
          .build();

      context.getLogger().log("Putting item into DynamoDB table: " + THERAPISTS_TABLE);
      dynamoDbClient.putItem(putItemRequest);
      context.getLogger().log("Successfully put item into DynamoDB");

      Therapist therapistResponse = new Therapist(therapistId, therapistRequest.getEmail(),
          therapistRequest.getSpecialization(), therapistRequest.getName());
      return createResponse(201, objectMapper.writeValueAsString(therapistResponse));
    } catch (ConditionalCheckFailedException e) {
      context.getLogger().log("Email already exists: " + e.getMessage());
      return createResponse(400, "{\"message\":\"Email already exists\"}");
    } catch (Exception e) {
      context.getLogger().log("Error in createClient: " + e.getMessage());
      context.getLogger().log("Exception type: " + e.getClass().getName());
      if (e.getCause() != null) {
        context.getLogger().log("Cause: " + e.getCause().getMessage());
      }
      return createResponse(500, "{\"message\":\"Error creating client: " + e.getMessage() + "\"}");
    }
  }

  // Helper method to create API Gateway responses.
  private APIGatewayProxyResponseEvent createResponse(int statusCode, String body) {
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
    response.setStatusCode(statusCode);
    response.setBody(body);

    // Add CORS headers
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Access-Control-Allow-Origin", "*");
    headers.put("Access-Control-Allow-Methods", "OPTIONS,POST,GET");
    headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
    response.setHeaders(headers);

    return response;
  }
}
