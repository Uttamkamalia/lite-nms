package com.motadata.nms.models;

import java.util.List;

public class MetricGroup {
  private Integer id;
  private String name;
  private List<String> metrics;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getMetrics() {
    return metrics;
  }

  public void setMetrics(List<String> metrics) {
    this.metrics = metrics;
  }
}

