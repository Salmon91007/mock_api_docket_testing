package com.ust.sdet.mock_api.oms;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.State;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;

import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Provider("OMS")
@PactBroker(
        host = "127.0.0.1",
        port = "9292"
)

public class OmsProviderVerificationTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startServer() {

        wireMockServer = new WireMockServer(4016);
        wireMockServer.start();

        configureFor("127.0.0.1",4016);
    }

    @AfterAll
    static void stopServer() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setup(PactVerificationContext context) {

        context.setTarget(new HttpTestTarget("127.0.0.1",wireMockServer.port()));

//        wireMockServer.resetAll();
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context){
        context.verifyInteraction();
    }


//    States
    @State("Order with ID 100 exists")
    public void orderExists(){

        wireMockServer.stubFor(get(urlEqualTo("/orders/100"))
                .willReturn(okJson("""
                        {
                          "id":100,
                          "ben":10,
                          "category":"mobile",
                          "status":"CONFIRMED"
                        }
                        """)));
    }


    @State("Order can be created")
    public void createOrder(){

        wireMockServer.stubFor(post(urlEqualTo("/orders"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type","application/json")
                        .withBody("""
                                {
                                  "id":100,
                                  "status":"CREATED"
                                }
                                """)));
    }

    @State("Inventory exists for SKU-9")
    public void inventoryExists(){

        wireMockServer.stubFor(get(urlEqualTo("/inventory/SKU-9"))
                .willReturn(okJson("""
                        {
                          "mobile":"Samsung",
                          "stock":60,
                          "address":"India"
                        }
                        """)));
    }
}