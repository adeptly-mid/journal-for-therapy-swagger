package com.lambda.messages;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Message;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TherapistGetMessagesHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  // MESSAGES_TABLE should be set in the Lambda environment variables
  private final String tableName = System.getenv("MESSAGES_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      // Retrieve clientId and therapistId from the path parameters (order based on
      // the endpoint)
      Map<String, String> pathParams = event.getPathParameters();
      String therapistId = pathParams.get("therapistId");
      String clientId = pathParams.get("clientId");
      // Construct a conversation ID (for example, "clientId#therapistId")
      String conversationId = clientId + "#" + therapistId;

      // Query the DynamoDB table using conversationId as the partition key
      QueryRequest queryReq = QueryRequest.builder()
          .tableName(tableName)
          .keyConditionExpression("conversationId = :cid")
          .expressionAttributeValues(
              Collections.singletonMap(":cid", AttributeValue.builder().s(conversationId).build()))
          .build();
      QueryResponse response = dynamoDb.query(queryReq);

      List<Message> messages = new ArrayList<>();
      for (Map<String, AttributeValue> item : response.items()) {
        messages.add(convertItemToMessage(item));
      }

      String responseBody = mapper.writeValueAsString(messages);
      return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error in TherapistGetMessagesHandler: " + e.getMessage());
      return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.getMessage());
    }
  }

  /**
   * Helper method to convert a DynamoDB item into a Message object.
   */
  private Message convertItemToMessage(Map<String, AttributeValue> item) {
    Message msg = new Message();
    if (item.get("messageId") != null) {
      msg.setMessageId(item.get("messageId").s());
    }
    if (item.get("sender") != null) {
      msg.setSender(item.get("sender").s());
    }
    if (item.get("receiver") != null) {
      msg.setReceiver(item.get("receiver").s());
    }
    if (item.get("content") != null) {
      msg.setContent(item.get("content").s());
    }
    if (item.get("createdAt") != null) {
      msg.setCreatedAt(item.get("createdAt").s());
    }
    return msg;
  }
}
