package com.therapy;

import software.amazon.awscdk.App;

public class TherapyJournalApp {
  public static void main(final String[] args) {
    App app = new App();
    new TherapyJournalStack(app, "TherapyJournalStack");
    app.synth();
  }
}
