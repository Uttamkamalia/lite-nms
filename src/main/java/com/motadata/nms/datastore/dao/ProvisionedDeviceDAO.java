package com.motadata.nms.datastore.dao;

import com.motadata.nms.models.ProvisionedDevice;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;

public class ProvisionedDeviceDAO {

  private final Pool pool;

  public ProvisionedDeviceDAO(Pool pool) {
    this.pool = pool;
  }

  // Create
  public Future<Void> save(ProvisionedDevice ProvisionedDevice) {
    String query = "INSERT INTO motadata.provisioned_devices (ip, port, discovery_profile_id, credentials_profile_id, hostname, os, device_type, status) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)";
    return pool.preparedQuery(query)
      .execute(Tuple.of(
        ProvisionedDevice.ip(),
        ProvisionedDevice.port(),
        ProvisionedDevice.discoveryProfileId(),
        ProvisionedDevice.credentialsProfileId(),
        ProvisionedDevice.hostname(),
        ProvisionedDevice.os(),
        ProvisionedDevice.deviceType(),
        ProvisionedDevice.status()
      ))
      .mapEmpty();
  }

  // Read
  public Future<ProvisionedDevice> get(Integer id) {
    String query = "SELECT * FROM motadata.provisioned_devices WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .map(rowSet -> {
        Row row = rowSet.iterator().next();
        return new ProvisionedDevice(
          row.getInteger("id"),
          row.getString("ip"),
          row.getInteger("port"),
          row.getInteger("discovery_profile_id"),
          row.getInteger("credentials_profile_id"),
          row.getString("hostname"),
          row.getString("os"),
          row.getString("device_type"),
          row.getString("status"),
          row.getString("discovered_at"));
      });
  }

  // Update
//  public Future<Void> update(ProvisionedDevice ProvisionedDevice) {
//    String query = "UPDATE motadata.provisioned_devices SET ip = $1, port = $2, discovery_profile_id = $3, credentials_profile_id = $4, hostname = $5, os = $6, device_type = $7, status = $8, discovered_at = $9 WHERE id = $10";
//    return pool.preparedQuery(query)
//      .execute(Tuple.of(
//        ProvisionedDevice.getIp(),
//        ProvisionedDevice.getPort(),
//        ProvisionedDevice.getDiscoveryProfileId(),
//        ProvisionedDevice.getCredentialsProfileId(),
//        ProvisionedDevice.getHostname(),
//        ProvisionedDevice.getOs(),
//        ProvisionedDevice.getDeviceType(),
//        ProvisionedDevice.getStatus(),
//        ProvisionedDevice.getDiscoveredAt(),
//        ProvisionedDevice.getId()
//      ))
//      .mapEmpty();
//  }

  // Delete
//  public Future<Void> delete(Integer id) {
//    String query = "DELETE FROM motadata.provisioned_devices WHERE id = $1";
//    return pool.preparedQuery(query)
//      .execute(Tuple.of(id))
//      .mapEmpty();
//  }
}
