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
                .onSuccess(row -> {
                    String token = row.getString("token");
                    sesService.sendConfirmation(email, token)
                            .onFailure(err -> Log.error(err));

                    ctx.response()
                            .setStatusCode(201)
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject().put("ok", true).encode());
                })
                .onFailure(err -> {
                    // unique violation: already subscribed, respond silently
                    if (err.getMessage() != null && err.getMessage().contains("unique")) {
                        ctx.response()
                                .setStatusCode(200)
                                .putHeader("Content-Type", "application/json")
                                .end(new JsonObject().put("ok", true).encode());
                    } else {
                        Log.error(err);
                        ctx.fail(500, err);
                    }
                });

    }
}