package com.harbourfront;

import com.harbourfront.database.DB;
import com.harbourfront.database.Database;
import com.harbourfront.services.SesService;
import com.harbourfront.services.SqsService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8081"));

        DB db = new DB(Database.createPool(vertx));
        SqsService sqsService = new SqsService();
        SesService sesService = new SesService();

        Router router = AppRouter.create(vertx, db, sqsService, sesService);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, result -> {
                    if (result.succeeded()) {
                        Log.info("Server listening on port " + port);
                        startPromise.complete();
                    } else {
                        startPromise.fail(result.cause());
                    }
                });
    }
}