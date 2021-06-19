package org.example.rs.http.retryingclient;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Slf4j
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
    public void get_withSuccessfulResponseAfter3Retries_returns200Ok() {
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
    public void get_withNonOkResponseOnEveryRetry_returnsSameResponse() {
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
    public void get_withSpecifiedDefaultRetryConfig_usesSpecifiedConfigAsDefault() {
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
                .retryOnResult(response -> !response.getStatusInfo().equals(Response.Status.OK))
                .build();
        HttpClient client = new HttpClient(customDefault);
        Response response = client.getWithRetries(
                GetRequest.builder().uri("http://localhost:8080/my/resource").acceptedResponse("text/xml").build(),
                null);
        assertTrue(response.getStatusInfo().equals(Response.Status.MOVED_PERMANENTLY));
    }

    @Test
    public void get_withConnectException_throwsSameException() {
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
    public void genericGet_withNotOkResponse_ThrowsExpectedException() {
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

    @Test
    public void get_withExponentialBackoff_retriesWorks() {
        long initialInterval = 1000L;
        double multiplier = 4.0D;
        int maxRetries = 5;

        RetryConfig expoBackoffConfig = RetryConfigHelper.expBackoffConfig(initialInterval, multiplier, maxRetries,
                null, null);

        HttpClient client = new HttpClient();
        client.addRetryConfig("expBackoff", expoBackoffConfig);

        AtomicInteger numAttempts = new AtomicInteger(1);
        Response response = client.getWithRetries(
                GetRequest.builder().uri("http://localhost:8080/my/resource").acceptedResponse("text/xml").build(),
                "expBackoff",
                () -> {
                    int currentAttempt = numAttempts.incrementAndGet();
                    log.info("Attempt {} initiated at {}", currentAttempt, Instant.now());
                    if (currentAttempt < 5) {
                        return Response.serverError().build();
                    } else {
                        return Response.ok().build();
                    }
                });
        assertTrue(response.getStatusInfo().equals(Response.Status.OK));
    }
}

class Dummy {
}
