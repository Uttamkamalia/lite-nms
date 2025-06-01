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
import java.util.Collection;
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
        // First, save the metric group
        String query = "INSERT INTO motadata.metric_group (name, device_type_id, polling_interval_seconds, status) " +
                       "VALUES ($1, $2, $3, $4) RETURNING id";

        return pool.preparedQuery(query)
            .execute(Tuple.of(
                metricGroup.getName(),
                metricGroup.getDeviceTypeId(),
                metricGroup.getPollingIntervalSeconds(),
                metricGroup.getStatus()
            ))
            .compose(rs -> {
                if (rs.iterator().hasNext()) {
                    Integer id = rs.iterator().next().getInteger("id");
                    metricGroup.setId(id);

                    // Now save the metric associations
                    return saveMetricAssociations(id, metricGroup.getMetrics())
                        .map(v -> id);
                }
                return Future.succeededFuture(null);
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to save metric group: " + metricGroup;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Helper method to save metric associations
    private Future<Integer> saveMetricAssociations(Integer metricGroupId, List<Integer> metricIds) {
        if (metricIds == null || metricIds.isEmpty()) {
            return Future.succeededFuture();
        }

        String query = "INSERT INTO motadata.metric_group_metrics (metric_group_id, metric_id) VALUES ($1, $2)";

        // Create a list of futures, one for each metric association
        List<Future<Integer>> futures = new ArrayList<>();

        for (Integer metricId : metricIds) {
            futures.add(
                pool.preparedQuery(query)
                    .execute(Tuple.of(metricGroupId, metricId))
                    .map(metricId)
            );
        }

        // Combine all futures and return a single future that completes when all insertions are done
        return Future.all(futures)
            .map(v -> metricGroupId)
            .recover(err -> {
                String errMsg = "Database Error: Failed to save metric associations for group: " + metricGroupId;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Read single
    public Future<JsonObject> get(Integer id) {
        String query = "SELECT mg.*, array_agg(mgm.metric_id) as metric_ids " +
                       "FROM motadata.metric_group mg " +
                       "LEFT JOIN motadata.metric_group_metrics mgm ON mg.id = mgm.metric_group_id " +
                       "WHERE mg.id = $1 " +
                       "GROUP BY mg.id";

        return pool.preparedQuery(query)
            .execute(Tuple.of(id))
            .map(rs -> {
                if (rs.iterator().hasNext()) {
                    Row row = rs.iterator().next();
                    JsonObject result = RowMapper.mapRowToJson(row);

                    // Convert array_agg result to JsonArray
                    Object metricIdsObj = result.getValue("metric_ids");
                    if (metricIdsObj != null && !metricIdsObj.toString().equals("[null]")) {
                        JsonArray metricIds = new JsonArray();
                        if (metricIdsObj instanceof Object[]) {
                            for (Object obj : (Object[]) metricIdsObj) {
                                if (obj != null) {
                                    metricIds.add(Integer.parseInt(obj.toString()));
                                }
                            }
                        }
                        result.put("metric_ids", metricIds);
                    } else {
                        result.put("metric_ids", new JsonArray());
                    }

                    return result;
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
        String query = "SELECT mg.*, array_agg(mgm.metric_id) as metric_ids " +
                       "FROM motadata.metric_group mg " +
                       "LEFT JOIN motadata.metric_group_metrics mgm ON mg.id = mgm.metric_group_id " +
                       "GROUP BY mg.id";

        return pool.preparedQuery(query)
            .execute()
            .map(rs -> {
                List<JsonObject> metricGroups = new ArrayList<>();
                for (Row row : rs) {
                    JsonObject result = RowMapper.mapRowToJson(row);

                    // Convert array_agg result to JsonArray
                    Object metricIdsObj = result.getValue("metric_ids");
                    if (metricIdsObj != null && !metricIdsObj.toString().equals("[null]")) {
                        JsonArray metricIds = new JsonArray();
                        if (metricIdsObj instanceof Object[]) {
                            for (Object obj : (Object[]) metricIdsObj) {
                                if (obj != null) {
                                    metricIds.add(Integer.parseInt(obj.toString()));
                                }
                            }
                        }
                        result.put("metric_ids", metricIds);
                    } else {
                        result.put("metric_ids", new JsonArray());
                    }

                    metricGroups.add(result);
                }
                return metricGroups;
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to retrieve all metric groups";
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Get by device type
    public Future<List<JsonObject>> getByDeviceType(Integer deviceTypeId) {
        String query = "SELECT mg.*, array_agg(mgm.metric_id) as metric_ids " +
                       "FROM motadata.metric_group mg " +
                       "LEFT JOIN motadata.metric_group_metrics mgm ON mg.id = mgm.metric_group_id " +
                       "WHERE mg.device_type_id = $1 " +
                       "GROUP BY mg.id";

        return pool.preparedQuery(query)
            .execute(Tuple.of(deviceTypeId))
            .map(rs -> {
                List<JsonObject> metricGroups = new ArrayList<>();
                for (Row row : rs) {
                    JsonObject result = RowMapper.mapRowToJson(row);

                    // Convert array_agg result to JsonArray
                    Object metricIdsObj = result.getValue("metric_ids");
                    if (metricIdsObj != null && !metricIdsObj.toString().equals("[null]")) {
                        JsonArray metricIds = new JsonArray();
                        if (metricIdsObj instanceof Object[]) {
                            for (Object obj : (Object[]) metricIdsObj) {
                                if (obj != null) {
                                    metricIds.add(Integer.parseInt(obj.toString()));
                                }
                            }
                        }
                        result.put("metric_ids", metricIds);
                    } else {
                        result.put("metric_ids", new JsonArray());
                    }

                    metricGroups.add(result);
                }
                return metricGroups;
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to retrieve metric groups for device type: " + deviceTypeId;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Update
    public Future<Integer> update(MetricGroup metricGroup) {
        if (metricGroup.getId() == null) {
            return Future.failedFuture(NMSException.badRequest("Metric Group ID is required for update"));
        }

        String query = "UPDATE motadata.metric_group SET name = $1, device_type_id = $2, " +
                       "polling_interval_seconds = $3, status = $4 WHERE id = $5";

        return pool.preparedQuery(query)
            .execute(Tuple.of(
                metricGroup.getName(),
                metricGroup.getDeviceTypeId(),
                metricGroup.getPollingIntervalSeconds(),
                metricGroup.getStatus(),
                metricGroup.getId()
            ))
            .compose(rs -> {
                if (rs.rowCount() == 0) {
                    return Future.failedFuture(NMSException.notFound("Metric group not found with id: " + metricGroup.getId()));
                }

                // Delete existing metric associations
                return deleteMetricAssociations(metricGroup.getId())
                    // Then save new ones
                    .compose(v -> saveMetricAssociations(metricGroup.getId(), metricGroup.getMetrics()))
                    .map(v -> metricGroup.getId());
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to update metric group: " + metricGroup;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Helper method to delete metric associations
    private Future<Integer> deleteMetricAssociations(Integer metricGroupId) {
        String query = "DELETE FROM motadata.metric_group_metrics WHERE metric_group_id = $1";

        return pool.preparedQuery(query)
            .execute(Tuple.of(metricGroupId))
            .map(v -> metricGroupId)
            .recover(err -> {
                String errMsg = "Database Error: Failed to delete metric associations for group: " + metricGroupId;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Update status
    public Future<Integer> updateStatus(Integer id, String status) {
        String query = "UPDATE motadata.metric_group SET status = $1 WHERE id = $2";

        return pool.preparedQuery(query)
            .execute(Tuple.of(status, id))
            .compose(rs -> {
                if (rs.rowCount() == 0) {
                    return Future.failedFuture(NMSException.notFound("Metric group not found with id: " + id));
                }
                return Future.succeededFuture(id);
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to update status for metric group: " + id;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    // Delete
    public Future<Integer> delete(Integer id) {
        // First delete the metric associations
        return deleteMetricAssociations(id)
            .compose(v -> {
                // Then delete the metric group
                String query = "DELETE FROM motadata.metric_group WHERE id = $1";

                return pool.preparedQuery(query)
                    .execute(Tuple.of(id))
                    .map(rs -> {
                        if (rs.rowCount() == 0) {
                            throw NMSException.notFound("Metric group not found with id: " + id);
                        }
                        return id;
                    });
            })
            .recover(err -> {
                String errMsg = "Database Error: Failed to delete metric group with id: " + id;
                log.error(errMsg, err);
                return Future.failedFuture(NMSException.internal(errMsg, err));
            });
    }

    public Future<JsonObject> getMetricGroupWithDetails(int metricGroupId) {
        String query = "SELECT " +
            "mg.id AS metric_group_id, " +
            "mg.name AS metric_group_name, " +
            "mg.polling_interval_seconds, " +
            "mg.last_polled_at, " +
            "mg.status, " +
            "dc.id AS device_catalog_id, " +
            "dc.type AS device_type, " +
            "dc.default_protocol, " +
            "dc.default_port, " +
            "dc.metadata AS device_metadata, " +
            "m.id AS metric_id, " +
            "m.name AS metric_name, " +
            "m.metric_type, " +
            "m.metric_unit, " +
            "m.protocol AS metric_protocol, " +
            "m.plugin_id " +
            "FROM motadata.metric_group mg " +
            "JOIN motadata.device_catalog dc ON mg.device_type_id = dc.id " +
            "JOIN motadata.metric_group_metrics mgm ON mg.id = mgm.metric_group_id " +
            "JOIN motadata.metric m ON mgm.metric_id = m.id " +
            "WHERE mg.id = $1";

        return pool.preparedQuery(query)
            .execute(Tuple.of(metricGroupId))
            .map(rows -> {
                JsonObject metricGroup = new JsonObject();
                JsonArray metrics = new JsonArray();

                for (Row row : rows) {
                    if (!metricGroup.containsKey("id")) {
                        metricGroup.put("id", row.getInteger("metric_group_id"))
                            .put("name", row.getString("metric_group_name"))
                            .put("polling_interval_seconds", row.getInteger("polling_interval_seconds"))
                            .put("last_polled_at", row.getValue("last_polled_at"))
                            .put("status", row.getString("status"));

                        JsonObject deviceCatalog = new JsonObject()
                            .put("id", row.getInteger("device_catalog_id"))
                            .put("type", row.getString("device_type"))
                            .put("default_protocol", row.getString("default_protocol"))
                            .put("default_port", row.getInteger("default_port"))
                            .put("metadata", row.getJsonObject("device_metadata"));

                        metricGroup.put("device_catalog", deviceCatalog);
                    }

                    JsonObject metric = new JsonObject()
                        .put("id", row.getInteger("metric_id"))
                        .put("name", row.getString("metric_name"))
                        .put("metric_type", row.getString("metric_type"))
                        .put("metric_unit", row.getString("metric_unit"))
                        .put("protocol", row.getString("metric_protocol"))
                        .put("plugin_id", row.getString("plugin_id"));

                    metrics.add(metric);
                }

                metricGroup.put("metrics", metrics);
                return metricGroup;
            })
            .recover(err -> {
                String msg = "Failed to fetch metric group with metrics and device info for id " + metricGroupId;
                log.error(msg, err);
                return Future.failedFuture(NMSException.internal(msg, err));
            });
    }

    public Future<JsonObject> getMetricGroupWithDetails(List<Integer> metricGroupIds) {
        String query = "SELECT " +
            "mg.id AS metric_group_id, " +
            "mg.name AS metric_group_name, " +
            "mg.polling_interval_seconds, " +
            "mg.last_polled_at, " +
            "mg.status, " +
            "dc.id AS device_catalog_id, " +
            "dc.type AS device_type, " +
            "dc.default_protocol, " +
            "dc.default_port, " +
            "dc.metadata AS device_metadata, " +
            "m.id AS metric_id, " +
            "m.name AS metric_name, " +
            "m.metric_type, " +
            "m.metric_unit, " +
            "m.protocol AS metric_protocol, " +
            "m.plugin_id " +
            "FROM motadata.metric_group mg " +
            "JOIN motadata.device_catalog dc ON mg.device_type_id = dc.id " +
            "JOIN motadata.metric_group_metrics mgm ON mg.id = mgm.metric_group_id " +
            "JOIN motadata.metric m ON mgm.metric_id = m.id " +
            "WHERE mg.id = ANY($1)";

        return pool.preparedQuery(query)
            .execute(Tuple.of(metricGroupIds))
            .map(rows -> {
                JsonObject metricGroups = new JsonObject();
                for (Row row : rows) {
                    Integer metricGroupId = row.getInteger("metric_group_id");
                    if (!metricGroups.containsKey(metricGroupId.toString())) {
                        JsonObject metricGroup = new JsonObject()
                            .put("id", metricGroupId)
                            .put("name", row.getString("metric_group_name"))
                            .put("polling_interval_seconds", row.getInteger("polling_interval_seconds"))
                            .put("last_polled_at", row.getValue("last_polled_at"))
                            .put("status", row.getString("status"));

                        JsonObject deviceCatalog = new JsonObject()
                            .put("id", row.getInteger("device_catalog_id"))
                            .put("type", row.getString("device_type"))
                            .put("default_protocol", row.getString("default_protocol"))
                            .put("default_port", row.getInteger("default_port"))
                            .put("metadata", row.getJsonObject("device_metadata"));

                        metricGroup.put("device_catalog", deviceCatalog);
                        metricGroup.put("metrics", new JsonArray());
                        metricGroups.put(metricGroupId.toString(), metricGroup);
                    }

                    JsonObject metric = new JsonObject()
                        .put("id", row.getInteger("metric_id"))
                        .put("name", row.getString("metric_name"))
                        .put("metric_type", row.getString("metric_type"))
                        .put("metric_unit", row.getString("metric_unit"))
                        .put("protocol", row.getString("metric_protocol"))
                        .put("plugin_id", row.getString("plugin_id"));

                    metricGroups.getJsonObject(metricGroupId.toString()).getJsonArray("metrics").add(metric);
                }
                return metricGroups;
            })
            .recover(err -> {
                String msg = "Failed to fetch metric groups with metrics and device info for ids " + metricGroupIds;
                log.error(msg, err);
                return Future.failedFuture(NMSException.internal(msg, err));
            });
    }

    public Future<JsonObject> getMetricGroupWithDevices(int metricGroupId) {
        String query = "SELECT " +
            "mg.id AS metric_group_id, " +
            "mg.name AS metric_group_name, " +
            "mg.polling_interval_seconds, " +
            "mg.last_polled_at, " +
            "mg.status, " +
            "dc.id AS device_catalog_id, " +
            "dc.type AS device_type, " +
            "dc.default_protocol, " +
            "dc.default_port, " +
            "dc.metadata AS device_metadata, " +
            "m.id AS metric_id, " +
            "m.name AS metric_name, " +
            "m.metric_type, " +
            "m.metric_unit, " +
            "m.protocol AS metric_protocol, " +
            "m.plugin_id, " +
            "pd.id AS device_id, " +
            "pd.ip AS device_ip, " +
            "pd.port AS device_port, " +
            "pd.protocol AS device_protocol, " +
            "pd.status AS device_status, " +
            "pd.metadata AS device_metadata, " +
            "cp.id AS credential_profile_id, " +
            "cp.name AS credential_profile_name, " +
            "cp.credentials AS credential_profile_credentials " +
            "FROM motadata.metric_group mg " +
            "JOIN motadata.device_catalog dc ON mg.device_type_id = dc.id " +
            "JOIN motadata.metric_group_metrics mgm ON mg.id = mgm.metric_group_id " +
            "JOIN motadata.metric m ON mgm.metric_id = m.id " +
            "LEFT JOIN motadata.provisioned_devices pd ON pd.device_type_id = dc.id " +
            "LEFT JOIN motadata.credential_profile cp ON pd.credentials_profile_id = cp.id " +
            "WHERE mg.id = $1 AND pd.status = 'PROVISIONED'" ;

        return pool.preparedQuery(query)
            .execute(Tuple.of(metricGroupId))
            .map(rows -> {
                if (rows.size() == 0) {
                    throw NMSException.notFound("Metric group not found with id: " + metricGroupId);
                }

                JsonObject result = new JsonObject();
                JsonObject metricGroup = new JsonObject();
                JsonArray metrics = new JsonArray();
                JsonArray devices = new JsonArray();

                // Track devices we've already processed to avoid duplicates
                // since the same device will appear multiple times (once per metric)
                java.util.Set<Integer> processedDeviceIds = new java.util.HashSet<>();

                // Track metrics we've already processed to avoid duplicates
                java.util.Set<Integer> processedMetricIds = new java.util.HashSet<>();

                for (Row row : rows) {
                    // Process metric group info (only once)
                    if (metricGroup.isEmpty()) {
                        metricGroup.put("id", row.getInteger("metric_group_id"))
                            .put("name", row.getString("metric_group_name"))
                            .put("polling_interval_seconds", row.getInteger("polling_interval_seconds"))
                            .put("last_polled_at", row.getValue("last_polled_at"))
                            .put("status", row.getString("status"));

                        JsonObject deviceCatalog = new JsonObject()
                            .put("id", row.getInteger("device_catalog_id"))
                            .put("type", row.getString("device_type"))
                            .put("default_protocol", row.getString("default_protocol"))
                            .put("default_port", row.getInteger("default_port"))
                            .put("metadata", row.getJsonObject("device_metadata"));

                        metricGroup.put("device_type", deviceCatalog);
                    }

                    // Process metric (if not already processed)
                    Integer metricId = row.getInteger("metric_id");
                    if (!processedMetricIds.contains(metricId)) {
                        JsonObject metric = new JsonObject()
                            .put("id", metricId)
                            .put("name", row.getString("metric_name"))
                            .put("metric_type", row.getString("metric_type"))
                            .put("metric_unit", row.getString("metric_unit"))
                            .put("protocol", row.getString("metric_protocol"))
                            .put("plugin_id", row.getString("plugin_id"));

                        metrics.add(metric);
                        processedMetricIds.add(metricId);
                    }

                    // Process device (if not already processed and if device exists)
                    Integer deviceId = row.getInteger("device_id");
                    if (deviceId != null && !processedDeviceIds.contains(deviceId)) {
                        JsonObject device = new JsonObject()
                            .put("id", deviceId)
                            .put("ip", row.getString("device_ip"))
                            .put("port", row.getInteger("device_port"))
                            .put("protocol", row.getString("device_protocol"))
                            .put("status", row.getString("device_status"))
                            .put("metadata", row.getJsonObject("device_metadata"));

                        // Add credential profile info
                        if (row.getInteger("credential_profile_id") != null) {
                            JsonObject credentialProfile = new JsonObject()
                                .put("id", row.getInteger("credential_profile_id"))
                                .put("name", row.getString("credential_profile_name"))
                                .put("credential", row.getJsonObject("credential_profile_credentials"));

                            device.put("credential_profile", credentialProfile);
                        }

                        devices.add(device);
                        processedDeviceIds.add(deviceId);
                    }
                }

                metricGroup.put("metrics", metrics);
                result.put("metricGroup", metricGroup);
                result.put("devices", devices);
                result.put("deviceCount", devices.size());

                return result;
            })
            .recover(err -> {
                String msg = "Failed to fetch metric group with devices for id " + metricGroupId;
                log.error(msg, err);
                return Future.failedFuture(NMSException.internal(msg, err));
            });
    }
}

