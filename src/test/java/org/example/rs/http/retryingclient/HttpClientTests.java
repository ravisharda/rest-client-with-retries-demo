package org.example.rs.http.retryingclient;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.net.UnknownHostException;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpClientTests {
    public static WireMockServer wireMockServer = new WireMockServer(8080, 8081);

    @BeforeClass
    public static void startServer() {
        wireMockServer.start();
    }

    @AfterClass
    public static void stopServer() {
        wireMockServer.stop();
    }

    @Test
    public void retrySucceedsInThirdRetry() {
        configureFor("localhost", 8080);
        stubFor(get("/my/resource")
                .inScenario("Retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(300)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Step1")
        );

        stubFor(get("/my/resource")
                .inScenario("Retry")
                .whenScenarioStateIs("Step1")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Step2")
        );

        stubFor(get("/my/resource")
                .inScenario("Retry")
                .whenScenarioStateIs("Step2")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Step3")
        );

        HttpClient client = new HttpClient();
        Response response = client.getWithRetries(
                GetRequest.builder().uri("http://localhost:8080/my/resource").acceptedResponse("text/xml").build(),
                null);
        assertTrue(response.getStatusInfo().equals(Response.Status.OK));
    }

    @Test
    public void retryFailsForever() {
        configureFor("localhost", 8080);
        stubFor(get("/my/resource")
                .willReturn(aResponse()
                        .withStatus(Response.Status.MOVED_PERMANENTLY.getStatusCode())
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
        );

        HttpClient client = new HttpClient();
        Response response = client.getWithRetries(
                GetRequest.builder().uri("http://localhost:8080/my/resource").acceptedResponse("text/xml").build(),
                null);
        assertTrue(response.getStatusInfo().equals(Response.Status.MOVED_PERMANENTLY));
    }

    @Test
    public void usesProvidedDefaultRetryConfig() {
        configureFor("localhost", 8080);
        stubFor(get("/my/resource")
                .inScenario("Retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(300)
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Step1")
        );

        stubFor(get("/my/resource")
                .inScenario("Retry")
                .whenScenarioStateIs("Step1")
                .willReturn(aResponse()
                        .withStatus(Response.Status.MOVED_PERMANENTLY.getStatusCode())
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Step2")
        );

        stubFor(get("/my/resource")
                .inScenario("Retry")
                .whenScenarioStateIs("Step2")
                .willReturn(aResponse()
                        .withStatus(Response.Status.BAD_GATEWAY.getStatusCode())
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
                .willSetStateTo("Step3")
        );

        RetryConfig customDefault = RetryConfig.<Response>custom()
                .maxAttempts(2) // Will retry 1 times only
                .waitDuration(Duration.of(2, SECONDS))
                .retryOnResult(response -> {
                    return !response.getStatusInfo().equals(Response.Status.OK);
                })
                .build();
        HttpClient client = new HttpClient(customDefault);
        Response response = client.getWithRetries(
                GetRequest.builder().uri("http://localhost:8080/my/resource").acceptedResponse("text/xml").build(),
                null);
        assertTrue(response.getStatusInfo().equals(Response.Status.MOVED_PERMANENTLY));
    }

    @Test
    public void retryOnConnectException() {
        HttpClient client = new HttpClient();
        try {
            Response response = client.getWithRetries(
                    GetRequest.builder().uri("http://nonexistent").acceptedResponse("text/xml").build(),
                    null);
            fail();
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof UnknownHostException);
        }
    }

    @Test
    public void genericGetThrowsExceptionWhenResponseIsNotOk() {
        configureFor("localhost", 8080);
        stubFor(get("/my/resource")
                .willReturn(aResponse()
                        .withStatus(Response.Status.BAD_REQUEST.getStatusCode())
                        .withHeader("Content-Type", "text/xml")
                        .withBody("<response>Some content</response>"))
        );

        try {
            new HttpClient().getWithRetries(Dummy.class,
                    GetRequest.builder()
                            .uri("http://localhost:8080/my/resource")
                            .acceptedResponse("text/xml")
                            .build(),
                    null);
            fail("ResponseNotOkException not thrown");
        } catch (ResponseNotOkException e) {
            assertTrue(e.getResponse().getStatusInfo().equals(Response.Status.BAD_REQUEST));
        }
    }
}

class Dummy {
}
