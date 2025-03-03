package com.lambda.search;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.SearchResultItem;
import com.model.SearchResults;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TherapistSearchHandler
    implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  // Environment variables for the Journal table and JournalAccessStatus table
  private final String journalTable = System.getenv("JOURNAL_TABLE");
  private final String journalAccessStatusTable = System.getenv("JOURNAL_ACCESS_STATUS_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      String therapistId = event.getPathParameters().get("therapistId");
      String query = event.getQueryStringParameters().get("q").toLowerCase();

      // Step 1: Find all clients who have granted access to this therapist.
      // JournalAccessStatus table has partition key: clientId and sort key:
      // therapistId.
      // Since we don't have a GSI on therapistId, we'll scan the table and filter.
      ScanRequest accessScanRequest = ScanRequest.builder()
          .tableName(journalAccessStatusTable)
          .build();
      ScanResponse accessScanResponse = dynamoDb.scan(accessScanRequest);
      List<String> accessibleClientIds = new ArrayList<>();
      accessScanResponse.items().forEach(item -> {
        if (item.get("therapistId") != null && therapistId.equals(item.get("therapistId").s())) {
          if (item.get("clientId") != null) {
            accessibleClientIds.add(item.get("clientId").s());
          }
        }
      });

      List<SearchResultItem> resultItems = new ArrayList<>();
      // Step 2: For each accessible client, search their Journal entries for the
      // query.
      for (String clientId : accessibleClientIds) {
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":cid", AttributeValue.builder().s(clientId).build());
        ScanRequest scanRequest = ScanRequest.builder()
            .tableName(journalTable)
            .filterExpression("clientId = :cid")
            .expressionAttributeValues(expressionAttributeValues)
            .build();
        ScanResponse scanResponse = dynamoDb.scan(scanRequest);
        for (Map<String, AttributeValue> item : scanResponse.items()) {
          String feeling = item.get("feeling") != null ? item.get("feeling").s().toLowerCase() : "";
          String content = item.get("content") != null ? item.get("content").s().toLowerCase() : "";
          if (feeling.contains(query) || content.contains(query)) {
            SearchResultItem result = new SearchResultItem();
            result.setType("JournalEntry");
            result.setMatch("Found in journal entry (" + item.get("journalEntryId").s() + ") for client " + clientId);
            resultItems.add(result);
          }
        }
      }

      // (Optionally, add additional search logic for client details or session
      // notes.)

      SearchResults searchResults = new SearchResults();
      searchResults.setQuery(query);
      searchResults.setResults(resultItems);
      String responseBody = mapper.writeValueAsString(searchResults);
      return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error in TherapistSearchHandler: " + e.getMessage());
      return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.getMessage());
    }
  }
}
