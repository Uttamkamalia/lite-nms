package com.motadata.nms.models;

import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class DeviceType {
  private Integer id;
  private Type type;
  private Protocol defaultProtocol;
  private Integer defaultPort;
  private JsonObject metadata;

  // Enum for device types
  public enum Type {
    NETWORK_DEVICE("NETWORK_DEVICE"),
    LINUX("LINUX"),
    UNKNOWN("UNKNOWN");

    private final String value;
    private static final Map<String, Type> lookup = new HashMap<>();

    static {
      for (Type type : Type.values()) {
        lookup.put(type.getValue().toUpperCase(), type);
      }
    }

    Type(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return value;
    }

    public static Type fromString(String typeStr) {
      if (typeStr == null) return UNKNOWN;
      return lookup.getOrDefault(typeStr.toUpperCase(), UNKNOWN);
    }
  }

  // Enum for protocols
  public enum Protocol {
    SNMP("SNMP", 161, "snmp-plugin/snmp_plugin"),
    SSH("SSH", 2222, "ssh-plugin/ssh_plugin"),
    UNKNOWN("UNKNOWN", 0, "default_plugin");

    private final String value;
    private final int defaultPort;
    private final String pluginName;
    private static final Map<String, Protocol> lookup = new HashMap<>();

    static {
      for (Protocol protocol : Protocol.values()) {
        lookup.put(protocol.getValue().toUpperCase(), protocol);
      }
    }

    Protocol(String value, int defaultPort, String pluginName) {
      this.value = value;
      this.defaultPort = defaultPort;
      this.pluginName = pluginName;
    }

    public String getValue() {
      return value;
    }

    public Integer getDefaultPort() {
      return defaultPort;
    }

    public String getPluginName() {
      return pluginName;
    }

    @Override
    public String toString() {
      return value;
    }

    public static Protocol fromString(String protocolStr) {
      if (protocolStr == null) return UNKNOWN;
      return lookup.getOrDefault(protocolStr.toUpperCase(), UNKNOWN);
    }
  }

  // Default constructor
  public DeviceType() {
    this.metadata = new JsonObject();
    this.defaultProtocol = Protocol.UNKNOWN;
    this.defaultPort = 0;
  }

  // Constructor with type
  public DeviceType(Type type) {
    this(null, type, null, null, new JsonObject());
  }

  // Constructor with type string
  public DeviceType(String typeStr) {
    this(null, Type.fromString(typeStr), null, null, new JsonObject());
  }

  // Constructor with type and protocol
  public DeviceType(Type type, Protocol protocol) {
    this(null, type, protocol, protocol.getDefaultPort(), new JsonObject());
  }

  // Full constructor
  public DeviceType(Integer id, Type type, Protocol defaultProtocol, Integer defaultPort, JsonObject metadata) {
    this.id = id;
    this.type = type;
    this.defaultProtocol = defaultProtocol != null ? defaultProtocol : Protocol.UNKNOWN;
    this.defaultPort = defaultPort != null ? defaultPort : (defaultProtocol != null ? defaultProtocol.getDefaultPort() : 0);
    this.metadata = metadata != null ? metadata : new JsonObject();
  }

  // Getters and setters
  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setType(String typeStr) {
    this.type = Type.fromString(typeStr);
  }

  public Protocol getDefaultProtocol() {
    return defaultProtocol;
  }

  public void setDefaultProtocol(Protocol protocol) {
    this.defaultProtocol = protocol;
    // Update port if it's not set
    if (this.defaultPort == null || this.defaultPort == 0) {
      this.defaultPort = protocol.getDefaultPort();
    }
  }

  public void setDefaultProtocol(String protocolStr) {
    setDefaultProtocol(Protocol.fromString(protocolStr));
  }

  public Integer getDefaultPort() {
    return defaultPort;
  }

  public void setDefaultPort(Integer port) {
    this.defaultPort = port;
  }

  public JsonObject getMetadata() {
    return metadata;
  }

  public void setMetadata(JsonObject metadata) {
    this.metadata = metadata != null ? metadata : new JsonObject();
  }

  // Helper methods for metadata
  public DeviceType addMetadata(String key, Object value) {
    if (this.metadata == null) {
      this.metadata = new JsonObject();
    }
    this.metadata.put(key, value);
    return this;
  }

  public <T> T getMetadataValue(String key, Class<T> clazz) {
    if (this.metadata == null) return null;
    return (T) this.metadata.getValue(key, null);
  }

  // Conversion to JSON
  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("type", type.getValue())
      .put("defaultProtocol", defaultProtocol.getValue())
      .put("defaultPort", defaultPort);

    if (id != null) {
      json.put("id", id);
    }

    if (metadata != null) {
      json.put("metadata", metadata);
    }

    return json;
  }

  // Create from JSON
  public static DeviceType fromJson(JsonObject json) {
    if (json == null) {
      throw new IllegalArgumentException("DeviceType JSON cannot be null");
    }

      DeviceType deviceType = new DeviceType();

      // Validate and set type
      String typeStr = json.getString("type");
      if (typeStr == null || typeStr.isEmpty()) {
        throw new IllegalArgumentException("Device type is required");
      }
      Type type = Type.fromString(typeStr);
      if (type == Type.UNKNOWN && !typeStr.equalsIgnoreCase("UNKNOWN")) {
        throw new IllegalArgumentException("Invalid device type: " + typeStr);
      }
      deviceType.setType(type);

      // Validate and set protocol
      String protocolStr = json.getString("default_protocol");
      if (protocolStr == null || protocolStr.isEmpty()) {
        throw new IllegalArgumentException("Default protocol is required");
      }
      Protocol protocol = Protocol.fromString(protocolStr);
      if (protocol == Protocol.UNKNOWN && !protocolStr.equalsIgnoreCase("UNKNOWN")) {
        throw new IllegalArgumentException("Invalid protocol: " + protocolStr);
      }
      deviceType.setDefaultProtocol(protocol);

      // Set port if present, otherwise use protocol's default port
      Integer port = json.getInteger("default_port");
      if (port != null) {
        deviceType.setDefaultPort(port);
      } else {
        deviceType.setDefaultPort(protocol.getDefaultPort());
      }

      // Set metadata if present
      JsonObject metadata = json.getJsonObject("metadata");
      if (metadata != null) {
        deviceType.setMetadata(metadata);
      }

      return deviceType;
  }

  @Override
  public String toString() {
    return "DeviceType{" +
      "id=" + id +
      ", type=" + type +
      ", defaultProtocol=" + defaultProtocol +
      ", defaultPort=" + defaultPort +
      ", metadata=" + metadata +
      '}';
  }
}
