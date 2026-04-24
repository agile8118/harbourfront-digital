package com.harbourfront.handlers;

import com.harbourfront.Log;
import com.harbourfront.database.DB;
import com.harbourfront.services.SqsService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ContactHandler {

    private final DB         db;
    private final SqsService sqsService;

    public ContactHandler(DB db, SqsService sqsService) {
        this.db         = db;
        this.sqsService = sqsService;
    }

    public void handle(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("Invalid request body.");
            return;
        }

        String name    = body.getString("name",    "").trim();
        String email   = body.getString("email",   "").trim();
        String message = body.getString("message", "").trim();

        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            ctx.response().setStatusCode(400).end("name, email, and message are required.");
            return;
        }

        String forwarded = ctx.request().getHeader("x-forwarded-for");
        String ip = (forwarded != null && !forwarded.isBlank())
            ? forwarded
            : ctx.request().remoteAddress().host();

        db.insert("contact_submissions", new JsonObject()
                .put("name",       name)
                .put("email",      email)
                .put("message",    message)
                .put("ip_address", ip))
            .onSuccess(row -> {
                sqsService.sendContactNotification(name, email, message)
                    .onFailure(err -> Log.error(err));

                ctx.response()
                    .setStatusCode(201)
                    .putHeader("Content-Type", "application/json")
                    .end(new JsonObject().put("ok", true).encode());
            })
            .onFailure(err -> {
                Log.error(err);
                ctx.fail(500, err);
            });
    }
}