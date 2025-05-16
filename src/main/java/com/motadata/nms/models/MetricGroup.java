package com.motadata.nms.models;

import java.util.List;

public class MetricGroup {
  private Integer id;
  private String name;
  private List<Metric> metrics;

  // Default constructor
  public MetricGroup() {
  }

  // Constructor without id
  public MetricGroup(String name, List<Metric> metrics) {
    this(null, name, metrics);
  }

  // Full constructor
  public MetricGroup(Integer id, String name, List<Metric> metrics) {
    this.id = id;
    this.name = name;
    this.metrics = metrics;
  }

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

  public List<Metric> getMetrics() {
    return metrics;
  }

  public void setMetrics(List<Metric> metrics) {
    this.metrics = metrics;
  }

  @Override
  public String toString() {
    return "MetricGroup{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", metrics=" + metrics +
      '}';
  }
}

