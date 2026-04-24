package com.harbourfront.middleware;

import com.harbourfront.Log;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Logs every request in the format:
 *   IP -- METHOD /path STATUS statusMessage -- response-time: X ms
 */
public class RequestLoggerHandler implements Handler<RoutingContext> {

    public static RequestLoggerHandler create() {
        return new RequestLoggerHandler();
    }

    @Override
    public void handle(RoutingContext ctx) {
        long requestStart = System.currentTimeMillis();

        String forwarded = ctx.request().getHeader("x-forwarded-for");
        String ip = (forwarded != null && !forwarded.isBlank())
            ? forwarded
            : ctx.request().remoteAddress().host();

        ctx.response().endHandler(v -> {
            int    statusCode    = ctx.response().getStatusCode();
            String statusMessage = ctx.response().getStatusMessage();
            long   elapsed       = System.currentTimeMillis() - requestStart;

            Log.info(
                ip + " -- " +
                ctx.request().method() + " " +
                ctx.request().uri() + " " +
                statusCode + " " +
                statusMessage + " -- response-time: " +
                elapsed + " ms"
            );
        });

        ctx.next();
    }
}