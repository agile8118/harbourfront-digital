package com.harbourfront;

import com.harbourfront.database.DB;
import com.harbourfront.handlers.ConfirmHandler;
import com.harbourfront.handlers.ContactHandler;
import com.harbourfront.handlers.NewsletterHandler;
import com.harbourfront.middleware.RequestLoggerHandler;
import com.harbourfront.services.SesService;
import com.harbourfront.services.SqsService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class AppRouter {

    public static Router create(Vertx vertx, DB db, SqsService sqsService, SesService sesService) {
        Router router = Router.router(vertx);

        // Log every request before anything else runs
        router.route().handler(RequestLoggerHandler.create());

        // Parse JSON / form bodies for POST routes
        router.route("/api/*").handler(BodyHandler.create());

        // ── API routes ─────────────────────────────────────────────────────────
        ContactHandler contactHandler = new ContactHandler(db, sqsService);
        router.post("/api/contact").handler(contactHandler::handle);

        NewsletterHandler newsletterHandler = new NewsletterHandler(db, sesService);
        router.post("/api/newsletter").handler(newsletterHandler::handle);

        ConfirmHandler confirmHandler = new ConfirmHandler(db);
        router.get("/api/newsletter/confirm").handler(confirmHandler::handle);

        // ── Static files ───────────────────────────────────────────────────────
        // Serves everything inside the ./public directory (relative to CWD).
        // The jar must be launched from the project root so that public/ is found.
        router.route("/*").handler(
            StaticHandler.create("public").setIndexPage("index.html")
        );

        // ── Error handlers ─────────────────────────────────────────────────────
        router.errorHandler(404, ctx -> {
            if (!ctx.response().ended()) {
                ctx.response().setStatusCode(404).end("Not found.");
            }
        });

        router.errorHandler(500, ctx -> {
            if (!ctx.response().ended()) {
                Throwable failure = ctx.failure();
                if (failure != null) Log.error(failure);
                ctx.response().setStatusCode(500).end("Internal server error.");
            }
        });

        return router;
    }
}
