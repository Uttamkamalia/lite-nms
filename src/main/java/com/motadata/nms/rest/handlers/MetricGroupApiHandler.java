//package com.motadata.nms.rest;
//
//import com.motadata.nms.datastore.dao.MetricGroupDAO;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.web.Router;
//import io.vertx.ext.web.RoutingContext;
//
//public class MetricGroupApiHandler {
//  private final MetricGroupDAO dao;
//
//  public MetricGroupApiHandler(MetricGroupDAO metricGroupDAO) {
//    this.dao = metricGroupDAO;
//  }
//
//  public void registerRoutes(Router router) {
//    router.post("/metric-group").handler(this::create);
//    router.get("/metric-group/:id").handler(this::read);
//    router.put("/metric-group/:id").handler(this::update);
//    router.delete("/metric-group/:id").handler(this::delete);
//  }
//
//  private void create(RoutingContext ctx) {
//    JsonObject body = ctx.body().asJsonObject();
//    dao.create(body).onSuccess(id ->
//      ctx.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("id", id).encode())
//    ).onFailure(err ->
//      ctx.response().setStatusCode(500).end(err.getMessage())
//    );
//  }
//
//  private void read(RoutingContext ctx) {
//    int id = Integer.parseInt(ctx.pathParam("id"));
//    dao.get(id).onSuccess(result ->
//      ctx.response().putHeader("Content-Type", "application/json").end(result.encode())
//    ).onFailure(err ->
//      ctx.response().setStatusCode(404).end("Not found")
//    );
//  }
//
//  private void update(RoutingContext ctx) {
//    int id = Integer.parseInt(ctx.pathParam("id"));
//    JsonObject body = ctx.body().asJsonObject();
//    dao.update(id, body).onSuccess(res ->
//      ctx.response().setStatusCode(204).end()
//    ).onFailure(err ->
//      ctx.response().setStatusCode(500).end(err.getMessage())
//    );
//  }
//
//  private void delete(RoutingContext ctx) {
//    int id = Integer.parseInt(ctx.pathParam("id"));
//    dao.delete(id).onSuccess(res ->
//      ctx.response().setStatusCode(204).end()
//    ).onFailure(err ->
//      ctx.response().setStatusCode(500).end(err.getMessage())
//    );
//  }
//}
