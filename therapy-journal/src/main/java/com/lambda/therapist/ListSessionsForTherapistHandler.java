package com.lambda.therapist;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListSessionsForTherapistHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String sessionTable = System.getenv("SESSION_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      Map<String, String> pathParams = event.getPathParameters();
      if (pathParams == null || !pathParams.containsKey("therapistId")) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody("Missing therapistId in path parameters");
      }
      String therapistId = pathParams.get("therapistId");

      // Prepare the query on the GSI (GSI_Session_ByTherapist)
      Map<String, String> expressionAttributeNames = new HashMap<>();
      expressionAttributeNames.put("#tid", "therapistId");

      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      expressionAttributeValues.put(":therapistId", AttributeValue.builder().s(therapistId).build());

      QueryRequest queryRequest = QueryRequest.builder()
          .tableName(sessionTable)
          .indexName("GSI_Session_ByTherapist")
          .keyConditionExpression("#tid = :therapistId")
          .expressionAttributeNames(expressionAttributeNames)
          .expressionAttributeValues(expressionAttributeValues)
          .build();

      List<Map<String, AttributeValue>> items = dynamoDb.query(queryRequest).items();
      String responseBody = mapper.writeValueAsString(items);
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(200)
          .withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error listing sessions: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withBody("Internal server error");
    }
  }
}
