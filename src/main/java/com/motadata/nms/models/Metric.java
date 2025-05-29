package com.motadata.nms.models;

import com.motadata.nms.commons.NMSException;
import io.vertx.core.json.JsonObject;
import static com.motadata.nms.rest.utils.ErrorCodes.BAD_REQUEST;

public class Metric {
    private Integer id;
    private String name;
    private Integer deviceTypeId;
    private String protocol;
    private String pluginId;

    // Default constructor
    public Metric() {
    }

    // Constructor without ID
    public Metric(String name, Integer deviceTypeId, String protocol, String pluginId) {
        this(null, name, deviceTypeId, protocol, pluginId);
    }

    // Full constructor
    public Metric(Integer id, String name, Integer deviceTypeId, String protocol, String pluginId) {
        this.id = id;
        this.name = name;
        this.deviceTypeId = deviceTypeId;
        this.protocol = protocol;
        this.pluginId = pluginId;
    }

    // Getters and setters
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

    public Integer getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(Integer deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    // Convert to JSON
    public JsonObject toJson() {
        JsonObject json = new JsonObject()
            .put("name", name)
            .put("device_type_id", deviceTypeId)
            .put("protocol", protocol)
            .put("plugin_id", pluginId);

        if (id != null) {
            json.put("id", id);
        }

        return json;
    }

    // Create from JSON
    public static Metric fromJson(JsonObject json) {
        if (json == null) {
            throw new NMSException(BAD_REQUEST, "Metric JSON cannot be null");
        }

        try {
            Integer id = json.getInteger("id");

            String name = json.getString("name");
            if (name == null || name.isEmpty()) {
                throw new NMSException(BAD_REQUEST, "Metric name is required");
            }

            Integer deviceTypeId = json.getInteger("device_type_id");
            if (deviceTypeId == null) {
                throw new NMSException(BAD_REQUEST, "Device type ID is required");
            }

            String protocol = json.getString("protocol");
            if (protocol == null || protocol.isEmpty()) {
                throw new NMSException(BAD_REQUEST, "Protocol is required");
            }

            String pluginId = json.getString("plugin_id");
            if (pluginId == null || pluginId.isEmpty()) {
                throw new NMSException(BAD_REQUEST, "Plugin ID is required");
            }

            return new Metric(id, name, deviceTypeId, protocol, pluginId);
        } catch (NMSException e) {
            throw e;
        } catch (Exception e) {
            throw new NMSException(BAD_REQUEST, "Invalid metric data: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "Metric{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", deviceTypeId=" + deviceTypeId +
            ", protocol='" + protocol + '\'' +
            ", pluginId='" + pluginId + '\'' +
            '}';
    }
}
