package com.ust.sdet.mock_api;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import static io.restassured.RestAssured.given;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WireMockServiceVirtualizationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private HttpClient client;

    @BeforeEach
    void pointConsumerAtWireMock() {
        io.restassured.RestAssured.baseURI = wm.baseUrl();
        client = HttpClient.newHttpClient();
    }

    @Test
    @DisplayName("Stub Inventory")
    void stubInventoryTwoOutcomes() {

        wm.stubFor(
                get(urlPathEqualTo("/inventory/SKU-9"))
                        .willReturn(
                                jsonResponse("""
                                    {
                                      "sku":"SKU-9",
                                        "qty":5
                                    }
                                    """, 200)
                        )
        );

        wm.stubFor(
                get(urlPathEqualTo("/inventory/SKU-404"))
                        .willReturn(
                                jsonResponse("""
                                        {
                                        "error": "SKU_NOT_FOUND"
                                        }
                                        """,404)
                        )
        );
        wm.stubFor(
                get(urlPathEqualTo("/inventory/SKU-0"))
                        .willReturn(
                                jsonResponse("""
                                    {
                                      "error":"OUT_OF_STOCK"
                                    }
                                    """, 409)
                        )
        );

        given()
                .when()
                .get("/inventory/SKU-9")
                .then()
                .statusCode(200)
                .body("qty", equalTo(5));

        given()
                .when()
                .get("/inventory/SKU-0")
                .then()
                .statusCode(409)
                .body("error", equalTo("OUT_OF_STOCK"));

        given()
                .when()
                .get("/inventory/SKU-404")
                .then()
                .statusCode(404)
                .body("error", equalTo("SKU_NOT_FOUND"));


        wm.verify(
                exactly(1),
                getRequestedFor(
                        urlPathEqualTo("/inventory/SKU-9")
                )
        );
    }

    @Test
    @DisplayName("force a timeout")
    void shouldTimeoutForSlowService() throws Exception {

        wm.stubFor(
                get(urlPathEqualTo("/orders/slow"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withFixedDelay(3000)
                                        .withBody("OK")
                        )
        );

        HttpRequest shortTimeoutRequest = HttpRequest.newBuilder()
                .uri(URI.create(wm.baseUrl() + "/orders/slow"))
                .timeout(Duration.ofSeconds(1))
                .GET()
                .build();

        assertThrows(
                HttpTimeoutException.class,
                () -> client.send(shortTimeoutRequest, ofString())
        );

        HttpRequest longTimeoutRequest = HttpRequest.newBuilder()
                .uri(URI.create(wm.baseUrl() + "/orders/slow"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        var response = client.send(longTimeoutRequest, ofString());

        assertEquals(200, response.statusCode());
    }

    @Test
    @DisplayName("Stateful order status")
    void orderStatusFromPendingToConfirmed() {

        wm.stubFor(
                get(urlPathEqualTo("/orders/42"))
                        .inScenario("fulfilment")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(
                                okJson("""
                                    {
                                      "id":42,
                                      "status":"PENDING"
                                    }
                                    """)
                        )
                        .willSetStateTo("PROCESSING")
        );

        wm.stubFor(
                get(urlPathEqualTo("/orders/42"))
                        .inScenario("fulfilment")
                        .whenScenarioStateIs("PROCESSING")
                        .willReturn(
                                okJson("""
                            {
                              "id":42,
                              "status":"PROCESSING"
                            }
                            """)
                        )
                        .willSetStateTo("CONFIRMED")
        );

        wm.stubFor(
                get(urlPathEqualTo("/orders/42"))
                        .inScenario("fulfilment")
                        .whenScenarioStateIs("CONFIRMED")
                        .willReturn(
                                okJson("""
                                    {
                                      "id":42,
                                      "status":"CONFIRMED"
                                    }
                                    """)
                        )
        );

        given()
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"));

        given()
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .body("status", equalTo("PROCESSING"));

        given()
                .when()
                .get("/orders/42")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    @DisplayName("Verify POST request body")
    void verifyPostBody() {

        wm.stubFor(
                post(urlPathEqualTo("/orders"))
                        .withRequestBody(
                                matchingJsonPath("$.product")
                        )
                        .willReturn(
                                jsonResponse("""
                                {
                                  "orderId":101,
                                  "status":"CREATED"
                                }
                                """, 201)
                        )
        );


        given()
                .contentType("application/json")
                .body("""
                {
                  "product":"Laptop",
                  "qty":2
                }
                """)
                .when()
                .post("/orders")
                .then()
                .statusCode(201)
                .body("status", equalTo("CREATED"));

        wm.verify(
                exactly(1),
                postRequestedFor(
                        urlPathEqualTo("/orders")
                )
        );
    }
}