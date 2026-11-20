package com.shoplite.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** The stock_reservations table: what is currently reserved under which id. */
@Component
class StockReservations {

    private final JdbcClient jdbc;
    private final JsonMapper json;

    StockReservations(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    Optional<List<ReservedProduct>> productsOf(UUID id) {
        return jdbc.sql("select products from stock_reservations where id = :id")
                .param("id", id)
                .query(String.class)
                .optional()
                .map(products -> json.readValue(products, new TypeReference<List<ReservedProduct>>() { }));
    }

    void save(UUID id, List<StockLine> lines, List<ReservedProduct> products) {
        jdbc.sql("""
                insert into stock_reservations (id, lines, products, created_at)
                values (:id, cast(:lines as jsonb), cast(:products as jsonb), now())""")
                .param("id", id)
                .param("lines", json.writeValueAsString(lines))
                .param("products", json.writeValueAsString(products))
                .update();
    }

    /** Deletes the reservation and returns its lines, or nothing if there is no such reservation. */
    Optional<List<StockLine>> remove(UUID id) {
        return jdbc.sql("delete from stock_reservations where id = :id returning lines")
                .param("id", id)
                .query(String.class)
                .optional()
                .map(lines -> json.readValue(lines, new TypeReference<List<StockLine>>() { }));
    }

}
