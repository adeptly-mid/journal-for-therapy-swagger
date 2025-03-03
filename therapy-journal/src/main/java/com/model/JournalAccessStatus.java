package com.model;

public class JournalAccessStatus {
  private String clientId;
  private String therapistId;
  private String access;

  public JournalAccessStatus() {
  }

  public JournalAccessStatus(String clientId, String therapistId, String access) {
    this.clientId = clientId;
    this.therapistId = therapistId;
    this.access = access;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getTherapistId() {
    return therapistId;
  }

  public void setTherapistId(String therapistId) {
    this.therapistId = therapistId;
  }

  public String getAccess() {
    return access;
  }

  public void setAccess(String access) {
    this.access = access;
  }
}
