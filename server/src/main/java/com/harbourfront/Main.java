package com.harbourfront;

import com.harbourfront.database.Seed;
import io.vertx.core.Vertx;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        // All application errors go to logs/YYYY-MM-DD/errors.log via Log.java.
        Logger.getLogger("").setLevel(Level.SEVERE);

        // If "seed" is passed as an argument, run the database seeding script and exit.
        if (args.length > 0 && args[0].equals("seed")) {
            Seed.run();
            return;
        }

        // Start the Vert.x application by deploying the MainVerticle.
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(), result -> {
            if (result.failed()) {
                System.out.println("ERROR: Failed to start — " + result.cause().getMessage());
                System.exit(1);
            }
        });
    }
}