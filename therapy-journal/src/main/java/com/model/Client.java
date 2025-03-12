package com.model;

public class Client {
  private String clientId;
  private String email;
  private String name;

  public Client() {
  }

  public Client(String clientId, String email, String name) {
    this.clientId = clientId;
    this.email = email;
    this.name = name;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
