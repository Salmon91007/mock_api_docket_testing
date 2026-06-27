package com.ust.sdet.mock_api.pos;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(pactVersion = PactSpecVersion.V4)
public class PosOmsConsumerPactTest {

//    Pact1
    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact createPactForExistingOrder(PactDslWithProvider builder) {

        return builder
                .given("Order with ID 100 exists")
                .uponReceiving("Get Order")
                .path("/orders/100")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .integerType("id", 100)
                        .integerType("ben",10)
                        .stringType("category","mobile")
                        .stringType("status", "CONFIRMED"))
                .toPact(V4Pact.class);
    }


    @Test
    @PactTestFor(pactMethod = "createPactForExistingOrder")
    void verifyOrder(MockServer mockServer) throws IOException {

        given()
                .baseUri(mockServer.getUrl())
                .when()
                .get("/orders/100")
                .then()
                .statusCode(200)
                .body("id", equalTo(100))
                .body("category",equalTo("mobile"))
                .body("status", equalTo("CONFIRMED"));
    }

//    Pact2
    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact createOrderPact(PactDslWithProvider builder) {

        return builder
                .given("Order can be created")
                .uponReceiving("Create Order")
                .path("/orders")
                .method("POST")
                .willRespondWith()
                .status(201)
                .body(new PactDslJsonBody()
                        .integerType("id", 100)
                        .stringType("status", "CREATED"))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "createOrderPact")
    void createOrder(MockServer mockServer) {

        given()
                .baseUri(mockServer.getUrl())
                .when()
                .post("/orders")
                .then()
                .statusCode(201)
                .body("id", equalTo(100))
                .body("status", equalTo("CREATED"));
    }


//    Pact3
    @Pact(provider = "OMS", consumer = "POS")
    public V4Pact getInventoryPact(PactDslWithProvider builder) {

        return builder
                .given("Inventory exists for SKU-9")
                .uponReceiving("Get Inventory")
                .path("/inventory/SKU-9")
                .method("GET")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringType("mobile", "Samsung")
                        .integerType("stock", 60)
                        .stringType("address","India")
                )
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getInventoryPact")
    void getInventory(MockServer mockServer) {

        given()
                .baseUri(mockServer.getUrl())
                .when()
                .get("/inventory/SKU-9")
                .then()
                .statusCode(200)
                .body("mobile", equalTo("Samsung"))
                .body("stock", equalTo(60))
                .body("address",equalTo("India"));
    }


}