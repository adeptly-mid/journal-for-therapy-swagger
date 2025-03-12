package com.common;

import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.util.HashMap;
import java.util.Map;

public class Utils {

  /**
   * Generates a unique client ID using UUID.
   * 
   * @return a unique client identifier.
   */
  public static String generateClientId() {
    return UUID.randomUUID().toString();
  }

  /**
   * Hashes the given plaintext password.
   * 
   * @param password the plaintext password.
   * @return the hashed password.
   */
  public static String hashPassword(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt(10));
  }

  /**
   * Verifies a plaintext password against a stored hash.
   * 
   * @param plainPassword  the plaintext password.
   * @param hashedPassword the stored hash.
   * @return true if the passwords match, false otherwise.
   */
  public static boolean verifyPassword(String plainPassword, String hashedPassword) {
    return BCrypt.checkpw(plainPassword, hashedPassword);
  }

  /**
   * Checks if an item with the provided (keyName, keyValue) exists in the
   * specified DynamoDB table.
   * 
   * @param dynamoDb  The DynamoDbClient instance
   * @param tableName The name of the DynamoDB table
   * @param keyName   The name of the primary key field
   * @param keyValue  The value of the primary key to look for
   * @return true if the item exists, false otherwise
   */
  public static boolean itemExistsInTable(DynamoDbClient dynamoDb, String tableName, String keyName, String keyValue) {
    Map<String, AttributeValue> keyMap = new HashMap<>();
    keyMap.put(keyName, AttributeValue.builder().s(keyValue).build());

    GetItemRequest getItemRequest = GetItemRequest.builder()
        .tableName(tableName)
        .key(keyMap)
        .build();

    GetItemResponse getItemResponse = dynamoDb.getItem(getItemRequest);
    return getItemResponse.hasItem();
  }
}
