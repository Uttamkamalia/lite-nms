package com.motadata.nms.models;

import com.motadata.nms.commons.NMSException;
import io.vertx.core.json.JsonObject;
import static com.motadata.nms.rest.utils.ErrorCodes.BAD_REQUEST;

public class Metric {
    private Integer id;
    private String name;
    private String metricType; // New field for COUNTER, GAUGE, etc.
    private String metricUnit; // New field for bytes, seconds, etc.
    private Integer deviceTypeId;
    private String protocol;
    private String pluginId;

    // Default constructor
    public Metric() {
    }

    // Constructor without ID
    public Metric(String name, String metricType, String metricUnit, Integer deviceTypeId, String protocol, String pluginId) {
        this(null, name, metricType, metricUnit, deviceTypeId, protocol, pluginId);
    }

    // Full constructor
    public Metric(Integer id, String name, String metricType, String metricUnit, Integer deviceTypeId, String protocol, String pluginId) {
        this.id = id;
        this.name = name;
        this.metricType = metricType;
        this.metricUnit = metricUnit;
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

    public String getMetricType() {
        return metricType;
    }

    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }

    public String getMetricUnit() {
        return metricUnit;
    }

    public void setMetricUnit(String metricUnit) {
        this.metricUnit = metricUnit;
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
            .put("metric_type", metricType)
            .put("metric_unit", metricUnit)
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

            String metricType = json.getString("metric_type");
            if (metricType == null || metricType.isEmpty()) {
                throw new NMSException(BAD_REQUEST, "Metric type is required");
            }

            String metricUnit = json.getString("metric_unit");
            if (metricUnit == null || metricUnit.isEmpty()) {
                throw new NMSException(BAD_REQUEST, "Metric unit is required");
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

            return new Metric(id, name, metricType, metricUnit, deviceTypeId, protocol, pluginId);
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
            ", metricType='" + metricType + '\'' +
            ", metricUnit='" + metricUnit + '\'' +
            ", deviceTypeId=" + deviceTypeId +
            ", protocol='" + protocol + '\'' +
            ", pluginId='" + pluginId + '\'' +
            '}';
    }
}
