package com.motadata.nms.datastore.dao;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.RowMapper;
import com.motadata.nms.models.ProvisionedDevice;
import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.motadata.nms.rest.utils.ErrorCodes.DAO_ERROR;

public class ProvisionedDeviceDAO {
  private static final Logger log = LoggerFactory.getLogger(ProvisionedDeviceDAO.class);
  private final Pool pool;

  public ProvisionedDeviceDAO(Pool pool) {
    this.pool = pool;
  }

  // Create
  public Future<Integer> save(ProvisionedDevice device) {
    String query = "INSERT INTO motadata.provisioned_devices " +
                   "(ip, port, protocol, discovery_profile_id, credentials_profile_id, " +
                   "device_type_id, metadata, status, discovered_at) " +
                   "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9) " +
                   "RETURNING id";

    return pool.preparedQuery(query)
      .execute(Tuple.of(
        device.getIpAddress(),
        device.getPort(),
        device.getProtocol(),
        device.getDiscoveryProfileId(),
        device.getCredentialProfileId(),
        device.getDeviceTypeId(),
        device.getMetadata() != null ? device.getMetadata() : new JsonObject(),
        device.getStatus(),
        device.getDiscoveredAt() != null ? device.getDiscoveredAt() : Instant.now()
      ))
      .map(rs -> {
        if (rs.iterator().hasNext()) {
          Integer id = rs.iterator().next().getInteger("id");
          device.setId(id);
          return id;
        }
        return null;
      })
      .recover(err -> {
        String errMsg = "Database Error: Failed to save provisioned device: " + device;
        log.error(errMsg, err);
        return Future.failedFuture(NMSException.internal(errMsg, err));
      });
  }

  // Read single
  public Future<JsonObject> get(Integer id) {
    String query = "SELECT * FROM motadata.provisioned_devices WHERE id = $1";

    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .compose(rowSet -> {
        if (rowSet == null || !rowSet.iterator().hasNext()) {
          return Future.failedFuture(NMSException.notFound(DAO_ERROR + "Provisioned device not found with id: " + id));
        }

        try {
          Row row = rowSet.iterator().next();
          return Future.succeededFuture(RowMapper.mapRowToJson(row));
        } catch (Exception e) {
          return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map provisioned device row to object for id: " + id, e));
        }
      })
      .onFailure(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to query provisioned device with id: " + id, err)));
  }

  // Read all
  public Future<List<ProvisionedDevice>> getAll() {
    String query = "SELECT * FROM motadata.provisioned_devices";

    return pool.preparedQuery(query)
      .execute()
      .map(rs -> {
        List<ProvisionedDevice> devices = new ArrayList<>();
        rs.forEach(row -> {
          try {
            devices.add(mapRowToProvisionedDevice(row));
          } catch (Exception e) {
            log.error("Error mapping provisioned device row", e);
          }
        });
        return devices;
      })
      .recover(err -> {
        log.error("Failed to query all provisioned devices", err);
        return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to query all provisioned devices", err));
      });
  }

  // Read as JsonArray
  public Future<JsonArray> getAllAsJson() {
    String query = "SELECT * FROM motadata.provisioned_devices";

    return pool.preparedQuery(query)
      .execute()
      .compose(rs -> {
        JsonArray results = new JsonArray();
        try {
          rs.forEach(row -> results.add(RowMapper.mapRowToJson(row)));
          return Future.succeededFuture(results);
        } catch (Exception e) {
          return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map provisioned device rows to JSON", e));
        }
      })
      .recover(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to query all provisioned devices", err)));
  }

  // Update
  public Future<ProvisionedDevice> update(ProvisionedDevice device) {
    if (device.getId() == null) {
      return Future.failedFuture(NMSException.badRequest("Device ID is required for update"));
    }

    String query = "UPDATE motadata.provisioned_devices SET " +
                   "ip = $1, port = $2, protocol = $3, discovery_profile_id = $4, " +
                   "credentials_profile_id = $5, device_type_id = $6, metadata = $7, " +
                   "status = $8 WHERE id = $9";

    return pool.preparedQuery(query)
      .execute(Tuple.of(
        device.getIpAddress(),
        device.getPort(),
        device.getProtocol(),
        device.getDiscoveryProfileId(),
        device.getCredentialProfileId(),
        device.getDeviceTypeId(),
        device.getMetadata() != null ? device.getMetadata() : new JsonObject(),
        device.getStatus(),
        device.getId()
      ))
      .compose(rs -> {
        if (rs.rowCount() == 0) {
          return Future.failedFuture(NMSException.notFound("Provisioned device not found with id: " + device.getId()));
        }
        return Future.succeededFuture(device);
      })
      .recover(err -> {
        String errMsg = "Database Error: Failed to update provisioned device: " + device;
        log.error(errMsg, err);
        return Future.failedFuture(NMSException.internal(errMsg, err));
      });
  }

  // Delete
  public Future<Integer> delete(Integer id) {
    String query = "DELETE FROM motadata.provisioned_devices WHERE id = $1";

    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .map(rs -> {
        if (rs.rowCount() == 0) {
          throw NMSException.notFound("Provisioned device not found with id: " + id);
        }
        return id;
      })
      .recover(err -> {
        String errMsg = "Database Error: Failed to delete provisioned device with id: " + id;
        log.error(errMsg, err);
        return Future.failedFuture(NMSException.internal(errMsg, err));
      });
  }

  // Find by IP
  public Future<ProvisionedDevice> findByIp(String ip) {
    String query = "SELECT * FROM motadata.provisioned_devices WHERE ip = $1";

    return pool.preparedQuery(query)
      .execute(Tuple.of(ip))
      .compose(rowSet -> {
        if (rowSet == null || !rowSet.iterator().hasNext()) {
          return Future.failedFuture(NMSException.notFound("Provisioned device not found with IP: " + ip));
        }

        try {
          Row row = rowSet.iterator().next();
          return Future.succeededFuture(mapRowToProvisionedDevice(row));
        } catch (Exception e) {
          return Future.failedFuture(NMSException.internal("Failed to map provisioned device row for IP: " + ip, e));
        }
      })
      .recover(err -> Future.failedFuture(NMSException.internal("Failed to query provisioned device with IP: " + ip, err)));
  }

  // Find by discovery profile ID
  public Future<List<ProvisionedDevice>> findByDiscoveryProfileId(Integer discoveryProfileId) {
    String query = "SELECT * FROM motadata.provisioned_devices WHERE discovery_profile_id = $1";

    return pool.preparedQuery(query)
      .execute(Tuple.of(discoveryProfileId))
      .map(rs -> {
        List<ProvisionedDevice> devices = new ArrayList<>();
        rs.forEach(row -> {
          try {
            devices.add(mapRowToProvisionedDevice(row));
          } catch (Exception e) {
            log.error("Error mapping provisioned device row", e);
          }
        });
        return devices;
      })
      .recover(err -> {
        log.error("Failed to query provisioned devices by discovery profile ID: " + discoveryProfileId, err);
        return Future.failedFuture(NMSException.internal("Failed to query provisioned devices by discovery profile ID", err));
      });
  }

  // Helper method to map a database row to a ProvisionedDevice object
  private ProvisionedDevice mapRowToProvisionedDevice(Row row) {
    ProvisionedDevice device = new ProvisionedDevice();
    device.setId(row.getInteger("id"));
    device.setIpAddress(row.getString("ip"));
    device.setPort(row.getInteger("port"));
    device.setProtocol(row.getString("protocol"));
    device.setDiscoveryProfileId(row.getInteger("discovery_profile_id"));
    device.setCredentialProfileId(row.getInteger("credentials_profile_id"));
    device.setDeviceTypeId(row.getInteger("device_type_id"));

    // Handle JSON fields
    String deviceInfoStr = row.getString("metadata");
    if (deviceInfoStr != null && !deviceInfoStr.isEmpty()) {
      device.setMetadata(new JsonObject(deviceInfoStr));
    }

    device.setStatus(row.getString("status"));

    // Handle timestamp
    if (row.getLocalDateTime("discovered_at") != null) {
      device.setDiscoveredAt(Timestamp.from(row.getLocalDateTime("discovered_at").toInstant(java.time.ZoneOffset.UTC)));
    }

    return device;
  }

//  // Add the updateStatus method
//  public Future<Void> updateStatus(Integer id, String status) {
//    String query = "UPDATE motadata.provisioned_devices SET status = $1 WHERE id = $2";
//
//    return pool.preparedQuery(query)
//      .execute(Tuple.of(status, id))
//      .compose(rs -> {
//        if (rs.rowCount() == 0) {
//          return Future.failedFuture(NMSException.notFound("Provisioned device not found with id: " + id));
//        }
//        return Future.succeededFuture();
//      })
//      .onFailure(err -> {
//        String errMsg = "Database Error: Failed to update status for provisioned device id: " + id;
//        log.error(errMsg, err);
//        return Future.failedFuture(NMSException.internal(errMsg, err));
//      });
//  }

}
