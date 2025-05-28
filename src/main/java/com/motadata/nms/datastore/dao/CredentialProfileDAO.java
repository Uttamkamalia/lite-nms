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

import static com.motadata.nms.rest.utils.ErrorCodes.DAO_ERROR;

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
        String errMsg = "Database Error: Failed to save credential-profile:"+ profile;
        log.error(errMsg, err);
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
          return Future.failedFuture(NMSException.notFound(DAO_ERROR + "CredentialProfile not found"));
        }
        try {
          Row row = rowSet.iterator().next();
          return Future.succeededFuture(RowMapper.mapRowToJson(row));
        } catch (Exception e) {
          return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map credential-profile row to JSON for id:"+id, e));
        }
      })
      .onFailure(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to query device-type with id:"+id, err)));
  }

  // Read all
  public Future<JsonArray> getAll() {
    String query = "SELECT * FROM motadata.credential_profile";
    return pool.preparedQuery(query)
      .execute()
      .compose(rs -> {
        JsonArray results = new JsonArray();
        try{
          rs.forEach(row -> results.add(RowMapper.mapRowToJson(row)));
          return Future.succeededFuture(results);
        } catch (Exception e) {
          return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map Credential-profile row to JSON for get-all:", e));
        }
      })
      .onFailure(err -> NMSException.internal(DAO_ERROR + "Failed to query all credential-profile", err));
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
      .recover(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to delete credential-profile with id:"+id , err)));
  }
}
