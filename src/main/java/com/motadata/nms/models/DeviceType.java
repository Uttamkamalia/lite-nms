package com.motadata.nms.models;

public class DeviceType {
  private Integer id;
  private String type;

  public DeviceType() {
    // Default constructor
  }

  public DeviceType(String type) {
    this(null, type);
  }

  public DeviceType(Integer id, String type) {
    this.id = id;
    this.type = type;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return "DeviceType{" +
      "id=" + id +
      ", type='" + type + '\'' +
      '}';
  }
}

