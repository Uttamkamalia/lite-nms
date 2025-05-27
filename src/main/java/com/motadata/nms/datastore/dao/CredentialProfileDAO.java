package com.motadata.nms.datastore.dao;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.RowMapper;
import com.motadata.nms.models.credential.CredentialProfile;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CredentialProfileDAO {

  private static final Logger log = LoggerFactory.getLogger(CredentialProfileDAO.class);
  private final Pool pool;

  public CredentialProfileDAO(Pool pool) {
    this.pool = pool;
  }

  // Create
  public Future<String> save(CredentialProfile profile) {
    String query = "INSERT INTO motadata.credential_profile (name, device_type, credentials) VALUES ($1, $2, $3)";
    return pool.preparedQuery(query)
      .execute(Tuple.of(profile.getName(), profile.getDeviceTypeId(), profile.getCredential().toJson()))
      .map(v -> "created")
      .recover(err -> {
        log.error("Database error: "+err);
        return Future.failedFuture(NMSException.internal("Database Error", err));
      });
  }

  // Read single
  // TODO decrypt password while returning response
  public Future<JsonObject> get(Integer id) {
    String query = "SELECT * FROM motadata.credential_profile WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .compose(rowSet -> {
        if (rowSet == null || !rowSet.iterator().hasNext()) {
          return Future.failedFuture(NMSException.notFound("CredentialProfile not found"));
        }
        Row row = rowSet.iterator().next();
        return Future.succeededFuture(RowMapper.mapRowToJson(row));
      })
      .onFailure(err -> Future.failedFuture(NMSException.internal("Database Error:"+err.getCause(), err)));
  }

  // Read all
  public Future<JsonArray> getAll() {
    String query = "SELECT * FROM motadata.credential_profile";
    return pool.preparedQuery(query)
      .execute()
      .map(rs -> {
        JsonArray result = new JsonArray();
        rs.forEach(row -> result.add(RowMapper.mapRowToJson(row)));
        return result;
      })
      .recover(err -> Future.failedFuture(NMSException.internal("Database Error", err)));
  }

  // Update
  public Future<Integer> update(CredentialProfile profile) {
    String query = "UPDATE motadata.credential_profile SET name = $1, device_type = $2, credentials = $3 WHERE id = $4";
    return pool.preparedQuery(query)
      .execute(Tuple.of(profile.getName(), profile.getDeviceTypeId(), profile.getCredential(), profile.getId()))
      .map(v -> profile.getId())
      .recover(err -> Future.failedFuture(NMSException.internal("Database Error", err)));
  }

  // Delete
  public Future<Integer> delete(Integer id) {
    String query = "DELETE FROM motadata.credential_profile WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .map(v -> id)
      .recover(err -> Future.failedFuture(NMSException.internal("Database Error", err)));
  }
}
