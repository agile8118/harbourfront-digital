package com.harbourfront.handlers;

import com.harbourfront.Log;
import com.harbourfront.database.DB;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import java.util.UUID;

public class ConfirmHandler {

    private final DB db;

    public ConfirmHandler(DB db) {
        this.db = db;
    }

    public void handle(RoutingContext ctx) {
        String tokenParam = ctx.request().getParam("token");
        if (tokenParam == null || tokenParam.isBlank()) {
            ctx.response().setStatusCode(400).end("Missing token.");
            return;
        }

        UUID token;
        try {
            token = UUID.fromString(tokenParam);
        } catch (IllegalArgumentException e) {
            ctx.response().setStatusCode(400).end("Invalid token.");
            return;
        }

        db.find("SELECT * FROM subscribers WHERE token = $1", Tuple.of(token))
                .onSuccess(row -> {
                    if (row == null) {
                        ctx.response().setStatusCode(404).end("Invalid token.");
                        return;
                    }

                    if (Boolean.TRUE.equals(row.getBoolean("confirmed"))) {
                        serveConfirmed(ctx);
                        return;
                    }

                    db.update("subscribers",
                                    new JsonObject().put("confirmed", true),
                                    "token = $1", Tuple.of(token))
                            .onSuccess(n -> serveConfirmed(ctx))
                            .onFailure(err -> { Log.error(err); ctx.fail(500, err); });
                })
                .onFailure(err -> { Log.error(err); ctx.fail(500, err); });
    }

    private void serveConfirmed(RoutingContext ctx) {
        ctx.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "text/html; charset=UTF-8")
                .sendFile("public/confirmed.html");
    }
}