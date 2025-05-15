package com.motadata.nms.datastore.dao;

import com.motadata.nms.models.DiscoveryProfile;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;

public class DiscoveryProfileDAO {

  private final Pool pool;

  public DiscoveryProfileDAO(Pool pool) {
    this.pool = pool;
  }

  // Create
  public Future<Void> save(DiscoveryProfile discoveryProfile) {
    String query = "INSERT INTO motadata.discovery_profile (target_ips, port, credentials_profile_id) VALUES ($1, $2, $3)";
    return pool.preparedQuery(query)
      .execute(Tuple.of(discoveryProfile.targetIps(), discoveryProfile.port(), discoveryProfile.credentialsProfileId()))
      .mapEmpty();
  }

  // Read
  public Future<DiscoveryProfile> get(Integer id) {
    String query = "SELECT * FROM motadata.discovery_profile WHERE id = $1";

    return pool
      .preparedQuery(query)
      .execute(Tuple.of(id))
      .map(rowSet -> {
        Row row = rowSet.iterator().next();

        return new DiscoveryProfile(row.getInteger("id"),
          row.getJsonObject("target_ips"),
          row.getInteger("port"),
          row.getInteger("credentials_profile_id"));
      });
  }

  // Update
  public Future<Void> update(DiscoveryProfile discoveryProfile) {
    String query = "UPDATE motadata.discovery_profile SET target_ips = $1, port = $2, credentials_profile_id = $3 WHERE id = $4";
    return pool.preparedQuery(query)
      .execute(Tuple.of(discoveryProfile.targetIps(), discoveryProfile.port(), discoveryProfile.credentialsProfileId(), discoveryProfile.id()))
      .mapEmpty();
  }

  // Delete
  public Future<Void> delete(Integer id) {
    String query = "DELETE FROM motadata.discovery_profile WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .mapEmpty();
  }
}

