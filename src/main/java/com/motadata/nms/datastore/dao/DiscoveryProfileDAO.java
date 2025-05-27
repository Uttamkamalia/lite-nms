package com.motadata.nms.datastore.dao;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.RowMapper;
import com.motadata.nms.models.DiscoveryProfile;
import io.vertx.core.Future;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import static io.vertx.core.http.impl.HttpClientConnection.log;

public class DiscoveryProfileDAO {
  Logger logger = LoggerFactory.getLogger(DiscoveryProfileDAO.class);

  private final Pool pool;

  public DiscoveryProfileDAO(Pool pool) {
    this.pool = pool;
  }

  // Create
  public Future<Integer> save(DiscoveryProfile profile) {
    String query = "INSERT INTO motadata.discovery_profile (target, credentials_profile_id) VALUES ($1, $2) RETURNING id";
    return pool.preparedQuery(query)
      .execute(Tuple.of(profile.getTarget(), profile.getCredentialsProfileId()))
      .map(rs -> {
        if (rs.iterator().hasNext()) {
          return rs.iterator().next().getInteger("id");
        }
        return null;
      })
      .recover(err -> {
        logger.error("Failed to save device type", err);
        return Future.failedFuture(NMSException.internal( "Database Error", err));
      });
  }

  // Read single
  public Future<JsonObject> get(Integer id) {
    String query = "SELECT * FROM motadata.discovery_profile WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .compose(rowSet -> {
        if (rowSet == null || !rowSet.iterator().hasNext()) {
          return Future.failedFuture(NMSException.notFound("Discovery Profile not found"));
        }
        Row row = rowSet.iterator().next();
        return Future.succeededFuture(RowMapper.mapRowToJson(row));
      })
      .recover(err -> {
        if (err instanceof NMSException) {
          return Future.failedFuture(err);
        }
        return Future.failedFuture(NMSException.internal("Database Error", err));
      });
  }

  // Read all
  public Future<JsonArray> getAll() {
    String query = "SELECT * FROM motadata.discovery_profile";
    return pool.preparedQuery(query)
      .execute()
      .map(rs -> {
        JsonArray result = new JsonArray();
        rs.forEach(row -> result.add(RowMapper.mapRowToJson(row)));
        return result;
      })
      .recover(err -> {
        logger.error("Failed to get all discovery profiles", err);
        return Future.failedFuture(NMSException.internal("Database Error", err));
      });
  }

  // Delete
  public Future<Integer> delete(Integer id) {
    String query = "DELETE FROM motadata.discovery_profile WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .map(v -> id)
      .recover(err -> Future.failedFuture(NMSException.internal("Database Error", err)));
  }
}

