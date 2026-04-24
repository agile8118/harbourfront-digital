package com.harbourfront.database;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A mini framework to help with DB operations. All methods return Vert.x Futures so
 * callers stay non-blocking.
 */
public class DB {

    private final Pool pool;

    public DB(Pool pool) {
        this.pool = pool;
    }

    // ── find ───────────────────────────────────────────────────────────────────

    /**
     * Returns the first matching row as a JsonObject, or null if no rows.
     * When multiple rows are returned the first is used.
     */
    public Future<JsonObject> find(String sql, Tuple params) {
        return pool.preparedQuery(sql).execute(params)
            .map(rows -> {
                var iter = rows.iterator();
                return iter.hasNext() ? rowToJson(iter.next()) : null;
            });
    }

    public Future<JsonObject> find(String sql) {
        return find(sql, Tuple.tuple());
    }

    // ── findMany ───────────────────────────────────────────────────────────────

    /** Returns all matching rows as a JsonArray (empty array if none). */
    public Future<JsonArray> findMany(String sql, Tuple params) {
        return pool.preparedQuery(sql).execute(params)
            .map(DB::rowSetToArray);
    }

    public Future<JsonArray> findMany(String sql) {
        return findMany(sql, Tuple.tuple());
    }

    // ── insert ─────────────────────────────────────────────────────────────────

    /**
     * Builds and executes an INSERT … RETURNING * from a JsonObject of
     * column→value pairs.  Returns the inserted row.
     */
    public Future<JsonObject> insert(String table, JsonObject data) {
        List<String> cols = new ArrayList<>(data.fieldNames());
        List<Object> vals = cols.stream().map(data::getValue).collect(Collectors.toList());

        String columns      = String.join(", ", cols);
        String placeholders = IntStream.rangeClosed(1, cols.size())
            .mapToObj(i -> "$" + i)
            .collect(Collectors.joining(", "));

        String sql = "INSERT INTO " + table + " (" + columns + ") VALUES (" + placeholders + ") RETURNING *";

        return pool.preparedQuery(sql).execute(Tuple.tuple(vals))
            .map(rows -> {
                var iter = rows.iterator();
                return iter.hasNext() ? rowToJson(iter.next()) : null;
            });
    }

    // ── update ─────────────────────────────────────────────────────────────────

    /**
     * Builds and executes an UPDATE … SET … WHERE ….
     *
     * The whereClause uses $1, $2 … placeholders that are automatically
     * offset past the data columns.
     *
     * Example (data has 2 fields → where params start at $3 internally):
     *   db.update("users",
     *       new JsonObject().put("name","Joe").put("role","admin"),
     *       "id = $1",          ← caller writes $1 — it becomes $3 internally
     *       Tuple.of(userId))
     */
    public Future<Integer> update(String table, JsonObject data, String whereClause, Tuple whereParams) {
        List<String> cols    = new ArrayList<>(data.fieldNames());
        List<Object> allVals = new ArrayList<>(cols.stream().map(data::getValue).collect(Collectors.toList()));
        for (int i = 0; i < whereParams.size(); i++) allVals.add(whereParams.getValue(i));

        String setClause = IntStream.rangeClosed(1, cols.size())
            .mapToObj(i -> cols.get(i - 1) + " = $" + i)
            .collect(Collectors.joining(", "));

        // Shift where-clause placeholder numbers past the data columns
        String shiftedWhere = shiftPlaceholders(whereClause, cols.size());
        String sql = "UPDATE " + table + " SET " + setClause + " WHERE " + shiftedWhere;

        return pool.preparedQuery(sql).execute(Tuple.tuple(allVals))
            .map(RowSet::rowCount);
    }

    // ── delete ─────────────────────────────────────────────────────────────────

    /**
     * Executes DELETE … WHERE … RETURNING *.
     * Returns an array of deleted rows (empty if nothing was deleted).
     */
    public Future<JsonArray> delete(String table, String whereClause, Tuple params) {
        String sql = "DELETE FROM " + table + " WHERE " + whereClause + " RETURNING *";
        return pool.preparedQuery(sql).execute(params).map(DB::rowSetToArray);
    }

    // ── query ──────────────────────────────────────────────────────────────────

    /** Executes any SQL and returns all result rows as a JsonArray. */
    public Future<JsonArray> query(String sql, Tuple params) {
        return pool.preparedQuery(sql).execute(params).map(DB::rowSetToArray);
    }

    public Future<JsonArray> query(String sql) {
        return query(sql, Tuple.tuple());
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private static JsonObject rowToJson(Row row) {
        JsonObject obj = new JsonObject();
        for (int i = 0; i < row.size(); i++) {
            Object val = row.getValue(i);
            // Convert temporal types to ISO strings so they serialise cleanly
            if (val instanceof OffsetDateTime) val = val.toString();
            else if (val instanceof LocalDateTime) val = val.toString();
            else if (val instanceof LocalDate) val = val.toString();
            obj.put(row.getColumnName(i), val);
        }
        return obj;
    }

    private static JsonArray rowSetToArray(RowSet<Row> rows) {
        JsonArray arr = new JsonArray();
        for (Row row : rows) arr.add(rowToJson(row));
        return arr;
    }

    /** Rewrites $1→$(1+offset), $2→$(2+offset), … in a WHERE clause. */
    private static String shiftPlaceholders(String clause, int offset) {
        Matcher m = Pattern.compile("\\$(\\d+)").matcher(clause);
        return m.replaceAll(r -> "$" + (Integer.parseInt(r.group(1)) + offset));
    }
}