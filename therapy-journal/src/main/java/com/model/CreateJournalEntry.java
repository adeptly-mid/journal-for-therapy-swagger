package com.model;

public class CreateJournalEntry {
  private String feeling;
  private int intensity;
  private String timeOfEmotion;
  private String content;

  // No-argument constructor
  public CreateJournalEntry() {
  }

  // Parameterized constructor
  public CreateJournalEntry(String feeling, int intensity, String timeOfEmotion, String content) {
    this.feeling = feeling;
    this.intensity = intensity;
    this.timeOfEmotion = timeOfEmotion;
    this.content = content;
  }

  // Getters and setters
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

  public String getTimeOfEmotion() {
    return timeOfEmotion;
  }

  public void setTimeOfEmotion(String timeOfEmotion) {
    this.timeOfEmotion = timeOfEmotion;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
