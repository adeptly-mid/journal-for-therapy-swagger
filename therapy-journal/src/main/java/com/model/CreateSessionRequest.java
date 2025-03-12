package com.model;

public class CreateSessionRequest {
  private String therapistId;
  private String clientId;
  private String title;
  private String privateNotes;
  private String sharedNotes;
  private String status; // Enum: pending, approved, rejected, open, closed
  private String startTime; // ISO-8601 formatted date/time
  private String endTime; // ISO-8601 formatted date/time

  // Getters and Setters
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

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }
}
