package com.model;

public class Session {
  private String sessionId;
  private String therapistId;
  private String clientId;
  private String title;
  private String privateNotes;
  private String sharedNotes;
  private String status; // Enum: pending, approved, rejected, open, closed
  private String createdAt;
  // Optionally add startTime and endTime if needed

  // Getters and Setters
  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getTherapistId() {
    return therapistId;
  }

  public void setTherapistId(String therapistId) {
    this.therapistId = therapistId;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPrivateNotes() {
    return privateNotes;
  }

  public void setPrivateNotes(String privateNotes) {
    this.privateNotes = privateNotes;
  }

  public String getSharedNotes() {
    return sharedNotes;
  }

  public void setSharedNotes(String sharedNotes) {
    this.sharedNotes = sharedNotes;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
