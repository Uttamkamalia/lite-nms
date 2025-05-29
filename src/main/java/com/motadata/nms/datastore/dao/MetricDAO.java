package com.motadata.nms.datastore.dao;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.RowMapper;
import com.motadata.nms.models.Metric;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.motadata.nms.rest.utils.ErrorCodes.DAO_ERROR;

public class MetricDAO {
    private static final Logger log = LoggerFactory.getLogger(MetricDAO.class);
    private final Pool pool;
    
    public MetricDAO(Pool pool) {
        this.pool = pool;
    }
    
    // Create
    public Future<Integer> save(Metric metric) {
        String query = "INSERT INTO motadata.metric (name, device_type_id, protocol, plugin_id) " +
                       "VALUES ($1, $2, $3, $4) RETURNING id";
        
        return pool.preparedQuery(query)
            .execute(Tuple.of(
                metric.getName(),
                metric.getDeviceTypeId(),
                metric.getProtocol(),
                metric.getPluginId()
            ))
            .map(rs -> {
                if (rs.iterator().hasNext()) {
                    Integer id = rs.iterator().next().getInteger("id");
                    metric.setId(id);
                    return id;
                }
                return null;
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to save metric: " + metric;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }
    
    // Read single
    public Future<JsonObject> get(Integer id) {
        String query = "SELECT * FROM motadata.metric WHERE id = $1";
        
        return pool.preparedQuery(query)
            .execute(Tuple.of(id))
            .compose(rowSet -> {
                if (rowSet == null || !rowSet.iterator().hasNext()) {
                    return Future.failedFuture(NMSException.notFound(DAO_ERROR + "Metric not found with id: " + id));
                }
                
                try {
                    Row row = rowSet.iterator().next();
                    return Future.succeededFuture(RowMapper.mapRowToJson(row));
                } catch (Exception e) {
                    return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map metric row to JSON for id: " + id, e));
                }
            })
            .recover(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to query metric with id: " + id, err)));
    }
    
    // Read all
    public Future<JsonArray> getAll() {
        String query = "SELECT * FROM motadata.metric";
        
        return pool.preparedQuery(query)
            .execute()
            .compose(rs -> {
                JsonArray results = new JsonArray();
                try {
                    rs.forEach(row -> results.add(RowMapper.mapRowToJson(row)));
                    return Future.succeededFuture(results);
                } catch (Exception e) {
                    return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map metric rows to JSON", e));
                }
            })
            .recover(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to query all metrics", err)));
    }
    
    // Get metrics by device type
    public Future<JsonArray> getByDeviceType(Integer deviceTypeId) {
        String query = "SELECT * FROM motadata.metric WHERE device_type_id = $1";
        
        return pool.preparedQuery(query)
            .execute(Tuple.of(deviceTypeId))
            .compose(rs -> {
                JsonArray results = new JsonArray();
                try {
                    rs.forEach(row -> results.add(RowMapper.mapRowToJson(row)));
                    return Future.succeededFuture(results);
                } catch (Exception e) {
                    return Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to map metric rows to JSON for device type: " + deviceTypeId, e));
                }
            })
            .recover(err -> Future.failedFuture(NMSException.internal(DAO_ERROR + "Failed to query metrics for device type: " + deviceTypeId, err)));
    }
    
    // Update
    public Future<Integer> update(Metric metric) {
        if (metric.getId() == null) {
            return Future.failedFuture(NMSException.badRequest("Metric ID is required for update"));
        }
        
        String query = "UPDATE motadata.metric SET name = $1, device_type_id = $2, protocol = $3, plugin_id = $4 WHERE id = $5";
        
        return pool.preparedQuery(query)
            .execute(Tuple.of(
                metric.getName(),
                metric.getDeviceTypeId(),
                metric.getProtocol(),
                metric.getPluginId(),
                metric.getId()
            ))
            .map(rs -> {
                if (rs.rowCount() == 0) {
                    throw NMSException.notFound("Metric not found with id: " + metric.getId());
                }
                return metric.getId();
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to update metric: " + metric;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }
    
    // Delete
    public Future<Integer> delete(Integer id) {
        String query = "DELETE FROM motadata.metric WHERE id = $1";
        
        return pool.preparedQuery(query)
            .execute(Tuple.of(id))
            .map(rs -> {
                if (rs.rowCount() == 0) {
                    throw NMSException.notFound("Metric not found with id: " + id);
                }
                return id;
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to delete metric with id: " + id;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }
}