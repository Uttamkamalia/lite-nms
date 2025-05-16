package com.motadata.nms.models;

public class Metric {
    private String name;
    private String pluginId;
    
    // Default constructor
    public Metric() {
    }
    
    // Full constructor
    public Metric(String name, String pluginId) {
        this.name = name;
        this.pluginId = pluginId;
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPluginId() {
        return pluginId;
    }
    
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
    
    @Override
    public String toString() {
        return "Metric{" +
            "name='" + name + '\'' +
            ", pluginId='" + pluginId + '\'' +
            '}';
    }
}