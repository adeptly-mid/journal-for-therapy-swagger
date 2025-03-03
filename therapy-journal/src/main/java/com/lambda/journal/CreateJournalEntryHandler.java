package com.lambda.journal;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.CreateJournalEntry;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreateJournalEntryHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String JOURNAL_TABLE = System.getenv("JOURNAL_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Access-Control-Allow-Origin", "*"); // CORS header

    try {
      // Get the clientId from the path parameters
      String clientId = event.getPathParameters().get("clientId");

      // Read the incoming JSON body into the CreateJournalEntry POJO
      CreateJournalEntry entry = mapper.readValue(event.getBody(), CreateJournalEntry.class);

      // Check for required fields in the POJO (feeling, timeOfEmotion, content)
      if (entry.getFeeling() == null || entry.getTimeOfEmotion() == null || entry.getContent() == null) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withHeaders(headers)
            .withBody("{\"error\": \"Missing required fields\"}");
      }

      // Generate unique identifiers and timestamps
      String journalEntryId = UUID.randomUUID().toString();
      String createdAt = Instant.now().toString();

      // Prepare item for DynamoDB with all the necessary attributes
      Map<String, AttributeValue> item = new HashMap<>();
      item.put("clientId", AttributeValue.builder().s(clientId).build());
      item.put("journalEntryId", AttributeValue.builder().s(journalEntryId).build());
      item.put("feeling", AttributeValue.builder().s(entry.getFeeling()).build());
      item.put("intensity", AttributeValue.builder().n(String.valueOf(entry.getIntensity())).build());
      item.put("timeOfEmotion", AttributeValue.builder().s(entry.getTimeOfEmotion()).build());
      item.put("content", AttributeValue.builder().s(entry.getContent()).build());
      item.put("createdAt", AttributeValue.builder().s(createdAt).build());

      PutItemRequest putRequest = PutItemRequest.builder()
          .tableName(JOURNAL_TABLE)
          .item(item)
          .build();

      dynamoDb.putItem(putRequest);

      // Log the creation of the journal entry into the JournalAccessLogs table.
      // We log an event with accessType "createEntry"
      Map<String, AttributeValue> logItem = new HashMap<>();
      String logTimestamp = Instant.now().toString();
      String logId = UUID.randomUUID().toString();
      logItem.put("clientId", AttributeValue.builder().s(clientId).build());
      logItem.put("accessTimestamp", AttributeValue.builder().s(logTimestamp).build());
      logItem.put("logId", AttributeValue.builder().s(logId).build());
      // Here we use the clientId as userid because the client is creating the entry.
      logItem.put("userid", AttributeValue.builder().s(clientId).build());
      logItem.put("accessType", AttributeValue.builder().s("createEntry").build());
      logItem.put("journalEntryId", AttributeValue.builder().s(journalEntryId).build());
      Map<String, AttributeValue> details = new HashMap<>();
      details.put("message", AttributeValue.builder().s("Journal entry created successfully").build());
      logItem.put("details", AttributeValue.builder().m(details).build());

      PutItemRequest logRequest = PutItemRequest.builder()
          .tableName("JournalAccessLogs")
          .item(logItem)
          .build();
      dynamoDb.putItem(logRequest);

      String logMessage = String.format("Journal entry created: journalEntryId=%s, clientId=%s, createdAt=%s",
          journalEntryId, clientId, createdAt);
      context.getLogger().log(logMessage);

      return new APIGatewayProxyResponseEvent()
          .withStatusCode(201)
          .withHeaders(headers)
          .withBody("{\"message\": \"Journal entry created\"}");

    } catch (Exception e) {
      context.getLogger().log("Error: " + e.getMessage());
      return new APIGatewayProxyResponseEvent()
          .withStatusCode(500)
          .withHeaders(headers)
          .withBody("{\"error\": \"Internal Server Error\"}");
    }
  }
}
