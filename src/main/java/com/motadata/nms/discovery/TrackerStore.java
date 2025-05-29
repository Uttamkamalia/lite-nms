package com.motadata.nms.discovery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrackerStore {
  // Private static instance variable
  private static TrackerStore instance;

  // Private map to store trackers
  private final Map<Integer, DiscoveryResultTracker> trackerMap = new ConcurrentHashMap<>();

  // Private constructor to prevent instantiation
  private TrackerStore() {
    // Private constructor to enforce singleton pattern
  }

  // Public static method to get the singleton instance
  public static synchronized TrackerStore getInstance() {
    if (instance == null) {
      instance = new TrackerStore();
    }
    return instance;
  }

  // Instance methods to manipulate the tracker map
  public void put(Integer key, DiscoveryResultTracker value) {
    trackerMap.put(key, value);
  }

  public DiscoveryResultTracker get(Integer key) {
    return trackerMap.get(key);
  }

  public void remove(Integer key) {
    trackerMap.remove(key);
  }

  public boolean contains(Integer key) {
    return trackerMap.containsKey(key);
  }

  public int size() {
    return trackerMap.size();
  }

  public void clear() {
    trackerMap.clear();
  }
}
