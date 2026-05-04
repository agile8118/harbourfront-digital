package com.harbourfront.handlers;

import com.harbourfront.Log;
import com.harbourfront.database.DB;
import com.harbourfront.services.SesService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class NewsletterHandler {

    private final DB db;
    private final SesService sesService;

    public NewsletterHandler(DB db, SesService sesService) {
        this.db = db;
        this.sesService = sesService;
    }

    public void handle(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();

        if (body == null) {
            ctx.response().setStatusCode(400).end("Invalid body");
            return;
        }

        String email = body.getString("email", "").trim();

        if (email.isEmpty()) {
            ctx.response().setStatusCode(400).end("No email provided");
            return;
        }

        db.insert("subscribers", new JsonObject().put("email", email))
                .onSuccess(row -> handleInsertSuccess(ctx, row, email))
                .onFailure(err -> handleInsertFailure(ctx, err));
    }

    private void handleInsertSuccess(RoutingContext ctx, JsonObject row, String email) {
        String token = row.getString("token");
        sesService.sendConfirmation(email, token)
                .onSuccess(v -> sendSuccess(ctx, 200))
                .onFailure(err -> sendError(ctx, err));
    }

    private void handleInsertFailure(RoutingContext ctx, Throwable err) {
        if (err.getMessage() != null && err.getMessage().contains("unique")) {
            // User already subscribed, but we don't want to leak that info, just respond
            // with success
            sendSuccess(ctx, 200);
        } else {
            Log.error(err);
            ctx.fail(500, err);
        }
    }

    private void sendSuccess(RoutingContext ctx, int statusCode) {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("ok", true).encode());
    }

    private void sendError(RoutingContext ctx, Throwable err) {
        Log.error(err);
        ctx.fail(500, err);
    }
}