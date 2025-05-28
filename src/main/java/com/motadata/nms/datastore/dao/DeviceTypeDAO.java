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

import static com.motadata.nms.rest.utils.ErrorCodes.DAO_ERROR;

public class DeviceTypeDAO {
  private static final Logger log = LoggerFactory.getLogger(DeviceTypeDAO.class);
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
        String errMsg = "Database Error: Failed to save device-type:"+ deviceType;
        log.error(errMsg, err);
        return Future.failedFuture(NMSException.internal( errMsg, err));
      });
  }

  public Future<JsonObject> get(Integer id) {
    String query = "SELECT * FROM motadata.device_catalog WHERE id = $1";

    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .compose(rowSet -> {

        if (rowSet == null || !rowSet.iterator().hasNext()) {
          return Future.failedFuture(NMSException.notFound(DAO_ERROR + "Device-type not found with id: "+id));
        }
        try {
          Row row = rowSet.iterator().next();
          return Future.succeededFuture(RowMapper.mapRowToJson(row));
        } catch (Exception e) {
          return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map Device-type row to JSON for id:"+id, e));
        }

      })
      .onFailure(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to query device-type with id:"+id, err)));
  }

  public Future<JsonArray> getAll(){
    String query = "SELECT * FROM motadata.device_catalog;";

    return pool.preparedQuery(query)
      .execute()
      .compose(rs -> {
        JsonArray results = new JsonArray();
        try{
          rs.forEach(row -> results.add(RowMapper.mapRowToJson(row)));
          return Future.succeededFuture(results);
        } catch (Exception e) {
          return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map Device-type row to JSON for get-all:", e));
        }
      })
      .onFailure(err -> NMSException.internal(DAO_ERROR + "Failed to query all device-types", err));
  }

  // Delete
  public Future<Integer> delete(Integer id) {
    String query = "DELETE FROM motadata.device_catalog WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .map(v -> id)
      .recover(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to delete device-type with id:"+id , err)));
  }
}

