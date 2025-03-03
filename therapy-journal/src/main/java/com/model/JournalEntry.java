package com.model;

public class JournalEntry {
  private String journalEntryId;
  private String clientId;
  private String timeOfEmotion;
  private String feeling;
  private int intensity;
  private String createdAt;
  private String content;

  public String getJournalEntryId() {
    return journalEntryId;
  }

  public void setJournalEntryId(String journalEntryId) {
    this.journalEntryId = journalEntryId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getTimeOfEmotion() {
    return timeOfEmotion;
  }

  public void setTimeOfEmotion(String timeOfEmotion) {
    this.timeOfEmotion = timeOfEmotion;
  }

  public String getFeeling() {
    return feeling;
  }

  public void setFeeling(String feeling) {
    this.feeling = feeling;
  }

  public int getIntensity() {
    return intensity;
  }

  public void setIntensity(int intensity) {
    this.intensity = intensity;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
