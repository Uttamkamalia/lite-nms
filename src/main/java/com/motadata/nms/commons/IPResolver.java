package com.motadata.nms.commons;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class IPResolver {

  public static List<String> resolveTargetIps(String target) {
    List<String> ips = new ArrayList<>();

    if (target == null || target.isBlank()) {
      throw new IllegalArgumentException("Target cannot be null or empty");
    }

    try {
      // Handle range
      if (target.contains("-")) {
        String[] parts = target.split("-");
        if (parts.length != 2) {
          throw new IllegalArgumentException("Invalid IP range format: " + target);
        }

        String startIp = parts[0].trim();
        String endIp = parts[1].trim();

        long start = ipToLong(startIp);
        long end = ipToLong(endIp);

        if (start > end) {
          throw new IllegalArgumentException("Start IP is greater than End IP");
        }

        for (long ip = start; ip <= end; ip++) {
          ips.add(longToIp(ip));
        }
      } else {
        // Single IP or hostname (e.g., localhost)
        InetAddress resolved = InetAddress.getByName(target);
        ips.add(resolved.getHostAddress());
      }

    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Invalid target IP or hostname: " + target, e);
    }

    return ips;
  }

  private static long ipToLong(String ipAddress) throws UnknownHostException {
    String[] octets = ipAddress.split("\\.");
    if (octets.length != 4) throw new UnknownHostException("Invalid IP: " + ipAddress);

    long result = 0;
    for (String octet : octets) {
      int part = Integer.parseInt(octet);
      if (part < 0 || part > 255) throw new UnknownHostException("Invalid IP segment: " + part);
      result = (result << 8) + part;
    }
    return result;
  }

  private static String longToIp(long ip) {
    return String.format("%d.%d.%d.%d",
      (ip >> 24) & 0xFF,
      (ip >> 16) & 0xFF,
      (ip >> 8) & 0xFF,
      ip & 0xFF
    );
  }

  /**
   * Check if a string is a valid IP address
   * @param ip The string to check
   * @return True if the string is a valid IP address, false otherwise
   */
  public static boolean isValidIp(String ip) {
    if (ip == null || ip.isEmpty()) {
      return false;
    }

    try {
      String[] parts = ip.split("\\.");
      if (parts.length != 4) {
        return false;
      }

      for (String part : parts) {
        int value = Integer.parseInt(part);
        if (value < 0 || value > 255) {
          return false;
        }
      }

      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
