//package com.motadata.nms.rest.handlers;
//
//import com.motadata.nms.datastore.dao.DiscoveryProfileDAO;
//import com.motadata.nms.models.DiscoveryProfile;
//import io.vertx.core.json.JsonObject;
//import io.vertx.ext.web.Router;
//import io.vertx.ext.web.RoutingContext;
//import io.vertx.sqlclient.Pool;
//
//public class DiscoveryProfileApiHandler {
//  private final DiscoveryProfileDAO dao;
//
//  public DiscoveryProfileApiHandler(Pool pool) {
//    this.dao = new DiscoveryProfileDAO(pool);
//  }
//
//  public void registerRoutes(Router router) {
//    router.post("/discovery-profile").handler(this::create);
//    router.get("/discovery-profile/:id").handler(this::read);
//    router.put("/discovery-profile/:id").handler(this::update);
//    router.delete("/discovery-profile/:id").handler(this::delete);
//  }
//
//  private void create(RoutingContext ctx) {
//    JsonObject body = ctx.body().asJsonObject();
//    dao.save(DiscoveryProfile.fromJson(body)).onSuccess(id ->
//      ctx.response().putHeader("Content-Type", "application/json").end(new JsonObject().put("id", id).encode())
//    ).onFailure(err ->
//      ctx.response().setStatusCode(500).end(err.getMessage())
//    );
//  }
//
//  private void read(RoutingContext ctx) {
//    int id = Integer.parseInt(ctx.pathParam("id"));
//
//  }
//
//  private void update(RoutingContext ctx) {
//    int id = Integer.parseInt(ctx.pathParam("id"));
//    JsonObject body = ctx.body().asJsonObject();
//    dao.update(DiscoveryProfile.fromJson(body)).onSuccess(res ->
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
