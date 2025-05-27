package com.motadata.nms.datastore.dao;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.RowMapper;
import com.motadata.nms.models.DeviceType;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;

public class DeviceTypeDAO {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeviceTypeDAO.class);
  private static Logger logger = LoggerFactory.getLogger(DeviceTypeDAO.class);
  private final SqlClient pool;

  public DeviceTypeDAO(Pool pool) {
    this.pool = pool;
  }

  // Create
  public Future<Integer> save(DeviceType deviceType) {
    String query = "INSERT INTO motadata.device_catalog (type, default_protocol, default_port, metadata) VALUES ($1, $2, $3, $4)";
    return pool.preparedQuery(query)
      .execute(Tuple.of(deviceType.getType(), deviceType.getDefaultProtocol(), deviceType.getDefaultPort(), deviceType.getMetadata()))
      .map(v -> deviceType.getId())
      .recover(err -> {
        log.error("Failed to save device type", err);
        return Future.failedFuture(NMSException.internal( "Database Error", err));
      });
      }

  public Future<JsonObject> get(Integer id) {
    String query = "SELECT * FROM motadata.device_catalog WHERE id = $1";

    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .compose(rowSet -> {

        if (rowSet == null || !rowSet.iterator().hasNext()) {
          return Future.failedFuture(NMSException.notFound("Device-type not found"));
        }

        Row row = rowSet.iterator().next();
        return Future.succeededFuture(RowMapper.mapRowToJson(row));
      })
      .onFailure(err -> Future.failedFuture(NMSException.internal("Database Error", err)));
  }

  public Future<JsonArray> getAll(){
    String query = "SELECT * FROM motadata.device_catalog;";

    return pool.preparedQuery(query)
      .execute()
      .map(rs -> {
        JsonArray results = new JsonArray();

        rs.forEach(r -> results.add(RowMapper.mapRowToJson(r)));

        return results;
      })
      .recover(err -> Future.failedFuture(NMSException.internal("Database Error", err)));
  }

  // Delete
  public Future<Integer> delete(Integer id) {
    String query = "DELETE FROM motadata.device_catalog WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .map(v -> id)
      .recover(err -> Future.failedFuture(NMSException.internal("Internal Error", err)));
  }
}

