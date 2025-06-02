package com.motadata.nms.discovery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscoveryResultTrackerRegistry {
  private static DiscoveryResultTrackerRegistry instance;

  private final Map<Integer, DiscoveryResultTracker> trackerMap = new ConcurrentHashMap<>();

  private DiscoveryResultTrackerRegistry() {
  }

  public static synchronized DiscoveryResultTrackerRegistry getInstance() {
    if (instance == null) {
      instance = new DiscoveryResultTrackerRegistry();
    }
    return instance;
  }

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
