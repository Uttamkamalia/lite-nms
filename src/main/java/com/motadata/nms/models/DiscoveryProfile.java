package com.motadata.nms.models;

import io.vertx.core.json.JsonObject;

public class DiscoveryProfile {
  private Integer id;
  private String target;
  private Integer credentialsProfileId;

  // Default constructor
  public DiscoveryProfile() {
  }

  // Constructor without ID
  public DiscoveryProfile(String target, Integer credentialsProfileId) {
    this(null, target, credentialsProfileId);
  }

  // Full constructor
  public DiscoveryProfile(Integer id, String target, Integer credentialsProfileId) {
    this.id = id;
    this.target = target;
    this.credentialsProfileId = credentialsProfileId;
  }

  // Getters and setters
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public Integer getCredentialsProfileId() {
    return credentialsProfileId;
  }

  public void setCredentialsProfileId(Integer credentialsProfileId) {
    this.credentialsProfileId = credentialsProfileId;
  }

  /**
   * Convert this object to a JsonObject
   * @return JsonObject representation of this DiscoveryProfile
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("target", target)
      .put("credentials_profile_id", credentialsProfileId);

    if (id != null) {
      json.put("id", id);
    }

    return json;
  }

  /**
   * Create a DiscoveryProfile from a JsonObject
   * @param json The JsonObject containing discovery profile data
   * @return A new DiscoveryProfile instance
   * @throws IllegalArgumentException if the JSON is invalid or missing required fields
   */
  public static DiscoveryProfile fromJson(JsonObject json) {
    if (json == null) {
      throw new IllegalArgumentException("DiscoveryProfile JSON cannot be null");
    }

    String target = json.getString("target");
    if (target == null || target.isEmpty()) {
      throw new IllegalArgumentException("DiscoveryProfile target is required");
    }

    Integer credentialsProfileId = json.getInteger("credentials_profile_id");
    if (credentialsProfileId == null) {
      throw new IllegalArgumentException("DiscoveryProfile credentials_profile_id is required");
    }

    return new DiscoveryProfile(
      json.getInteger("id"),
      target,
      credentialsProfileId
    );
  }

  @Override
  public String toString() {
    return "DiscoveryProfile{" +
      "id=" + id +
      ", target='" + target + '\'' +
      ", credentialsProfileId=" + credentialsProfileId +
      '}';
  }
}

