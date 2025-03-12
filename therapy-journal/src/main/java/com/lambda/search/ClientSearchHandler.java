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

public class ClientSearchHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private final DynamoDbClient dynamoDb = DynamoDbClient.create();
  private final ObjectMapper mapper = new ObjectMapper();
  // Environment variable for the Journal table name
  private final String journalTable = System.getenv("JOURNAL_TABLE");

  @Override
  public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
    try {
      String clientId = event.getPathParameters().get("clientId");
      String query = event.getQueryStringParameters().get("q").toLowerCase();

      // Scan the Journal table for entries belonging to the client
      Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
      expressionAttributeValues.put(":cid", AttributeValue.builder().s(clientId).build());
      ScanRequest scanRequest = ScanRequest.builder()
          .tableName(journalTable)
          .filterExpression("clientId = :cid")
          .expressionAttributeValues(expressionAttributeValues)
          .build();
      ScanResponse scanResponse = dynamoDb.scan(scanRequest);

      List<SearchResultItem> resultItems = new ArrayList<>();
      // For each journal entry, check if the query appears in the 'feeling' or
      // 'content'
      scanResponse.items().forEach(item -> {
        String feeling = item.get("feeling") != null ? item.get("feeling").s().toLowerCase() : "";
        String content = item.get("content") != null ? item.get("content").s().toLowerCase() : "";
        if (feeling.contains(query) || content.contains(query)) {
          SearchResultItem result = new SearchResultItem();
          result.setType("JournalEntry");
          result.setMatch(
              "Journal entry ID: " + (item.get("journalEntryId") != null ? item.get("journalEntryId").s() : "unknown"));
          resultItems.add(result);
        }
      });

      SearchResults searchResults = new SearchResults();
      searchResults.setQuery(query);
      searchResults.setResults(resultItems);
      String responseBody = mapper.writeValueAsString(searchResults);
      return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(responseBody);
    } catch (Exception e) {
      context.getLogger().log("Error in ClientSearchHandler: " + e.getMessage());
      return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(e.getMessage());
    }
  }
}
