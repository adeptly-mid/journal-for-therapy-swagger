package com.model;

import java.util.List;

public class SearchResults {
  private String query;
  private List<SearchResultItem> results;

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public List<SearchResultItem> getResults() {
    return results;
  }

  public void setResults(List<SearchResultItem> results) {
    this.results = results;
  }
}
