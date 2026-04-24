package com.harbourfront.database;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class Database {

    public static Pool createPool(Vertx vertx) {
        PgConnectOptions connect = new PgConnectOptions()
            .setHost(env("DB_HOST", "localhost"))
            .setPort(Integer.parseInt(env("DB_PORT", "5432")))
            .setDatabase(env("DB_NAME", "harbourfront"))
            .setUser(env("DB_USER", "postgres"))
            .setPassword(env("DB_PASSWORD", ""));

        PoolOptions pool = new PoolOptions().setMaxSize(10);

        return PgPool.pool(vertx, connect, pool);
    }

    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}
