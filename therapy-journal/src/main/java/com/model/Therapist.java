package com.model;

public class Therapist {
  private String therapistId;
  private String email;
  private String specialization;
  private String name;

  public Therapist() {
  }

  public Therapist(String therapistId, String email, String specialization, String name) {
    this.therapistId = therapistId;
    this.email = email;
    this.specialization = specialization;
    this.name = name;
  }

  public String getTherapistId() {
    return therapistId;
  }

  public void setTherapistId(String therapistId) {
    this.therapistId = therapistId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getSpecialization() {
    return specialization;
  }

  public void setSpecialization(String specialization) {
    this.specialization = specialization;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
