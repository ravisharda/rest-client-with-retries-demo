package org.example.rs.http.retryingclient;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

import static java.time.temporal.ChronoUnit.SECONDS;

@RequiredArgsConstructor
@AllArgsConstructor
@Slf4j
/**
 * Wraps {@link Client} to provide HTTP invocation, while performing retries transparently.
 * Transparent retries allows the caller code to delegate complex retry handling logic
 * to an instance of this class.
 *
 * Note: The caller can specify the retry config to use for each HTTP call. If retry config is
 * unspecified, a default retry config is used. The caller may change the default retry
 * config.
 */
public class HttpClient {
    private static final String DEFAULT_RETRY_NAME = "defaultRetry";
    private final Client client;
    private final RetryRegistry retryRegistry;
    private int connectTimeout = (int)Duration.ofMinutes(30).toMillis();
    private int readTimeout = (int)Duration.ofMinutes(90).toMillis();

    public HttpClient() {
        retryRegistry = RetryRegistry.of(defaultRetryConfig());
        client = ClientBuilder.newBuilder()
                .property(ClientProperties.CONNECT_TIMEOUT, connectTimeout)
                .property(ClientProperties.READ_TIMEOUT, readTimeout)
                .build();
    }

    public HttpClient(RetryConfig defaultRetryConfig) {
        retryRegistry = RetryRegistry.of(defaultRetryConfig);
        client = ClientBuilder.newBuilder()
                .property(ClientProperties.CONNECT_TIMEOUT, connectTimeout)
                .property(ClientProperties.READ_TIMEOUT, readTimeout)
                .build();
    }

    /**
     * Enables callers to add a custom retry configuration to the retry registry.
     *
     * @param name
     * @param retryConfig
     */
    public void addRetryConfig(String name, RetryConfig retryConfig) {
        this.retryRegistry.retry(name, retryConfig);
    }

    /**
     * Invokes the specified {@code request}, then fetches and returns the
     * {@link Response}.
     *
     * If the invocation results in retryable error occurs, retries the request
     * based on the {@link Retry} linked with the specified {@code retryName}. If
     * an entry specified by the {@code retryName} is null, uses a default
     *
     * @param request
     * @param retryName
     * @return
     */
    public Response getWithRetries(GetRequest request, String retryName) {
        log.debug("uri = {}, retryName = {}", request.getUri(), retryName);
        Retry retry = fetchRetry(retryName);
        Supplier<Response> responseSupplier = Retry.decorateSupplier(retry,
                () -> {
                    Response result = get(request);
                    return result;
                });

        Response result = responseSupplier.get();
        Response.StatusType statusInfo = result.getStatusInfo();
        log.debug("Response status for uri {}: code = {}, family = {}, reason = {}", request.getUri(),
                statusInfo.getStatusCode(),
                statusInfo.getFamily(),
                statusInfo.getReasonPhrase());
        return result;
    }

    public <T> T getWithRetries(Class<T> clazz, GetRequest request,
                                String retryName) {
        Response response = getWithRetries(request, retryName);
        if (response.getStatusInfo().equals(Response.Status.OK)) {
            return response.readEntity(clazz);
        } else {
            throw new ResponseNotOkException(response);
        }
    }

    public <T> T get(Class<T> clazz, GetRequest request) {
        WebTarget target = client.target(URI.create(request.getUri()));
        Response response = target.request(request.getAcceptedResponse()).get();
        Response.StatusType statusType = response.getStatusInfo();
        if (response.getStatusInfo().equals(Response.Status.OK)) {
            return response.readEntity(clazz);
        } else {
            throw new ResponseNotOkException(response);
        }
    }

    /**
     * Creates a default {@link RetryConfig}.
     *
     * @return {@link RetryConfig}
     */
    private RetryConfig defaultRetryConfig() {
        return RetryConfig.<Response>custom()
                .maxAttempts(3) // Will retry 2 times
                .waitDuration(Duration.of(2, SECONDS))
                .retryOnResult(response -> !response.getStatusInfo().equals(Response.Status.OK)
                        && ResponseStatus.isRetryableError(response.getStatusInfo()))
                .retryOnException(e -> e.getCause() instanceof ConnectException)
                .build();
    }

    public Response get(GetRequest request) {
        WebTarget target = client.target(URI.create(request.getUri()));
        return target.request(request.getAcceptedResponse()).get();
    }

    /**
     * Returns a {@link Retry} object from the registry having the specified
     * {@code retryName}. If no item exists, returns an object based on the default
     * RetryConfig.
     *
     * @param retryName the name
     * @return the Retry instance
     */
    private Retry fetchRetry(String retryName) {
        Retry result = null;
        if (retryName == null || retryName.trim().equals("")) {
            result = retryRegistry.retry(DEFAULT_RETRY_NAME);
        } else {
            result = retryRegistry.retry(retryName);
        }
        result.getEventPublisher().onRetry(event ->
                log.info("Retrying. Num of retry attempts = {}", event.getNumberOfRetryAttempts()));
        return result;
    }
}
