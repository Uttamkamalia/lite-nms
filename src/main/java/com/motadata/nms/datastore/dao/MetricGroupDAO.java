package com.motadata.nms.datastore.dao;

import com.motadata.nms.commons.NMSException;
import com.motadata.nms.commons.RowMapper;
import com.motadata.nms.models.MetricGroup;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static com.motadata.nms.rest.utils.ErrorCodes.DAO_ERROR;

public class MetricGroupDAO {
    private static final Logger log = LoggerFactory.getLogger(MetricGroupDAO.class);
    private final Pool pool;

    public MetricGroupDAO(Pool pool) {
        this.pool = pool;
    }

    // Create
    public Future<Integer> save(MetricGroup metricGroup) {
        String query = "INSERT INTO motadata.metric_group (name, metrics, device_type_id, polling_interval_seconds, status) " +
                       "VALUES ($1, $2, $3, $4, $5) RETURNING id";

        return pool.preparedQuery(query)
            .execute(Tuple.of(
                metricGroup.getName(),
                metricGroup.getMetrics().toArray(new String[0]),
                metricGroup.getDeviceTypeId(),
                metricGroup.getPollingIntervalSeconds(),
                metricGroup.getStatus()
            ))
            .map(rs -> {
                if (rs.iterator().hasNext()) {
                    Integer id = rs.iterator().next().getInteger("id");
                    metricGroup.setId(id);
                    return id;
                }
                return null;
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to save metric group: " + metricGroup;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Read single
    public Future<JsonObject> get(Integer id) {
        String query = "SELECT * FROM motadata.metric_group WHERE id = $1";

        return pool.preparedQuery(query)
            .execute(Tuple.of(id))
            .map(rs -> {
                if (rs.iterator().hasNext()) {
                    Row row = rs.iterator().next();
                    return RowMapper.mapRowToJson(row);
                }
                return null;
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to retrieve metric group with id: " + id;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Read all
    public Future<List<JsonObject>> getAll() {
        String query = "SELECT * FROM motadata.metric_group";

        return pool.preparedQuery(query)
            .execute()
            .map(rs -> {
                List<JsonObject> metricGroups = new ArrayList<>();
                for (Row row : rs) {
                    metricGroups.add(RowMapper.mapRowToJson(row));
                }
                return metricGroups;
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to retrieve all metric groups";
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Delete
    public Future<Integer> delete(Integer id) {
        String query = "DELETE FROM motadata.metric_group WHERE id = $1";

        return pool.preparedQuery(query)
            .execute(Tuple.of(id))
            .map(v -> id)
            .recover(err -> {
                String errMsg = "Database Error: Failed to delete metric group with id: " + id;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }
}

