package com.lambda.messages;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Message;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TherapistSendMessageHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String tableName = System.getenv("MESSAGES_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      // Extract path parameters: clientId and therapistId.
      Map<String, String> pathParams = event.getPathParameters();
      String clientId = pathParams.get("clientId");
      String therapistId = pathParams.get("therapistId");
      String conversationId = clientId + "#" + therapistId;

      // Read the request body; therapist send message (no sender field needed)
      Map<String, Object> requestBody = mapper.readValue(event.getBody(), Map.class);
      String content = (String) requestBody.get("content");

      // Build the message (sender is the therapist)
      Message message = new Message();
      message.setMessageId(UUID.randomUUID().toString());
      message.setSender(therapistId);
      message.setReceiver(clientId);
      message.setContent(content);
      message.setCreatedAt(Instant.now().toString());

      // Prepare DynamoDB item
      Map<String, AttributeValue> item = new HashMap<>();
      item.put("conversationId", AttributeValue.builder().s(conversationId).build());
      item.put("createdAt", AttributeValue.builder().s(message.getCreatedAt()).build());
      item.put("messageId", AttributeValue.builder().s(message.getMessageId()).build());
      item.put("sender", AttributeValue.builder().s(message.getSender()).build());
      item.put("receiver", AttributeValue.builder().s(message.getReceiver()).build());
      item.put("content", AttributeValue.builder().s(message.getContent()).build());

      PutItemRequest putReq = PutItemRequest.builder()
          .tableName(tableName)
          .item(item)
          .build();
      dynamoDb.putItem(putReq);

      String responseBody = mapper.writeValueAsString(message);
      return new APIGatewayProxyResponseEvent().withStatusCode(201).withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error in TherapistSendMessageHandler: " + e.getMessage());
      return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.getMessage());
    }
  }
}
