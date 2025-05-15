package com.motadata.nms.datastore.dao;

import com.motadata.nms.models.MetricGroup;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.Future;
import io.vertx.sqlclient.Row;

public class MetricGroupDAO {

  private final Pool pool;

  public MetricGroupDAO(Pool pool) {
    this.pool = pool;
  }

  // Create
  public Future<Void> create(MetricGroup metricGroup) {
    String query = "INSERT INTO motadata.metric_group (name, metrics) VALUES ($1, $2)";
    return pool.preparedQuery(query)
      .execute(Tuple.of(metricGroup.getName(), metricGroup.getMetrics()))
      .mapEmpty();
  }

  // Read
  public Future<MetricGroup> get(Integer id) {
    String query = "SELECT * FROM motadata.metric_group WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .map(rowSet -> {
        Row row = rowSet.iterator().next();
        MetricGroup metricGroup = new MetricGroup();
        metricGroup.setId(row.getInteger("id"));
        metricGroup.setName(row.getString("name"));
        metricGroup.setMetrics(row.getJsonArray("metrics").getList());
        return metricGroup;
      });
  }

  // Update
  public Future<Void> update(MetricGroup metricGroup) {
    String query = "UPDATE motadata.metric_group SET name = $1, metrics = $2 WHERE id = $3";
    return pool.preparedQuery(query)
      .execute(Tuple.of(metricGroup.getName(), metricGroup.getMetrics(), metricGroup.getId()))
      .mapEmpty();
  }

  // Delete
  public Future<Void> delete(Integer id) {
    String query = "DELETE FROM motadata.metric_group WHERE id = $1";
    return pool.preparedQuery(query)
      .execute(Tuple.of(id))
      .mapEmpty();
  }
}

