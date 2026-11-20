package com.shoplite.loadtests;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/**
 * Customers searching the catalog: "name contains ..." with a random word, 20 results per page.
 * Ramps from 5 to 50 new users per second over 30 seconds, then holds 50 per second for 60 seconds.
 * Each virtual user sends one search. The target is the catalog itself (-DbaseUrl to change it).
 */
public class ProductSearchSimulation extends Simulation {

    private static final String[] WORDS = {"mug", "bottle", "notebook", "pen", "hoodie", "lamp", "chair", "desk"};

    private final Iterator<Map<String, Object>> searchTerms = Stream.generate(
            () -> Map.<String, Object>of("term", WORDS[ThreadLocalRandom.current().nextInt(WORDS.length)]))
            .iterator();

    private final HttpProtocolBuilder protocol = http
            .baseUrl(System.getProperty("baseUrl", "http://localhost:8281"))
            .acceptHeader("application/json");

    private final ScenarioBuilder search = scenario("Search products")
            .feed(searchTerms)
            .exec(http("search by name")
                    .get("/api/products?search=#{term}&size=20")
                    .check(status().is(200)));

    {
        setUp(search.injectOpen(
                rampUsersPerSec(5).to(50).during(30),
                constantUsersPerSec(50).during(60)))
                .protocols(protocol)
                .assertions(global().successfulRequests().percent().gt(99.0));
    }

}
