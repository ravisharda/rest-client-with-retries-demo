package org.example.rs.http.retryingclient;

import com.google.common.annotations.VisibleForTesting;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.function.Supplier;

@Slf4j
/**
 * Delegates HTTP API calls to {@link Client} and retries transparently upon failure
 * freeing up the caller from retries.
 *
 * This object may be configured with a default {@link RetryConfig}, otherwise it'll use
 * a built-in one. The {@link RetryConfig} used by a method depends on whether a {@code retryName}
 * is specified as an argument. If the {@code retryName} argument is non-null, the
 * corresponding {@link RetryConfig} from the Retry Registry is used, otherwise the default
 * instance is used.
 *
 */
public class HttpApiClient {
    private static final String DEFAULT_RETRY_NAME = "defaultRetry";
    private static final int DEFAULT_CONNECT_TIMEOUT = 30 * 60 * 1000; // 30 minutes
    private static final int DEFAULT_READ_TIMEOUT = 90 * 60 * 1000; // 90 minutes

    private final Client client;
    private final RetryRegistry retryRegistry;

    public HttpApiClient() {
        this(ClientBuilder.newBuilder()
                .property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT)
                .property(ClientProperties.READ_TIMEOUT, DEFAULT_READ_TIMEOUT)
                .build());
    }

    public HttpApiClient(@NonNull Client client) {
        this(client, RetryConfigHelper.regularIntervalConfig(3, 2,
                RetryConfigHelper.defaultRetryOnResponse(),
                RetryConfigHelper.defaultRetryOnException()));
    }

    public HttpApiClient(@NonNull Client client, @NonNull RetryConfig defaultRetryConfig) {
        this.retryRegistry = RetryRegistry.of(defaultRetryConfig);
        this.client = client;
    }

    public HttpApiClient(@NonNull RetryConfig defaultRetryConfig) {
        this(ClientBuilder.newBuilder()
                .property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT)
                .property(ClientProperties.READ_TIMEOUT, DEFAULT_READ_TIMEOUT)
                .build(), defaultRetryConfig);
    }

    /**
     * Enables callers to add a custom retry configuration to the retry registry.
     *
     * @param name the name of the registry entry
     * @param retryConfig the retry configuration
     */
    public void addRetryConfig(@NonNull String name, @NonNull RetryConfig retryConfig) {
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
     * @param request an object representing the HTTP GET request
     * @param retryName the name of the {@link RetryConfig} to use
     * @return
     */
    public Response getWithRetries(@NonNull GetRequest request, String retryName) {
        return getWithRetries(request, retryName, () -> {
            Response result = get(request);
            return result;
        });
    }

    @VisibleForTesting
    Response getWithRetries(@NonNull GetRequest request, String retryName, @NonNull Supplier<Response> responseSupp) {
        log.trace("GetRequest = {}, retryName = {}", request, retryName);
        Retry retry = fetchRetry(retryName);
        Supplier<Response> decoratedSupp = Retry.decorateSupplier(retry, responseSupp);

        Response result = decoratedSupp.get();
        Response.StatusType statusInfo = result.getStatusInfo();
        log.debug("Response status for uri {}: code = {}, family = {}, reason = {}", request.getTarget(),
                statusInfo.getStatusCode(),
                statusInfo.getFamily(),
                statusInfo.getReasonPhrase());
        return result;
    }

    /**
     * Invokes the specified request, retries based on specified {@code retryName}, then
     * unmarshalls the response into an object of the specified {@code clazz}.
     *
     * @param clazz
     * @param request
     * @param retryName
     * @param <T>
     * @return
     */
    public <T> T getWithRetries(@NonNull Class<T> clazz, @NonNull GetRequest request,
                                String retryName) {
        Response response = getWithRetries(request, retryName);
        if (response.getStatusInfo().equals(Response.Status.OK)) {
            return response.readEntity(clazz);
        } else {
            throw new ResponseNotOkException(response);
        }
    }

    public <T> T get(@NonNull Class<T> clazz, @NonNull GetRequest request) {
        WebTarget target = client.target(URI.create(request.getTarget()));
        Response response = target.request(request.getAcceptedResponse()).get();
        Response.StatusType statusType = response.getStatusInfo();
        if (response.getStatusInfo().equals(Response.Status.OK)) {
            return response.readEntity(clazz);
        } else {
            throw new ResponseNotOkException(response);
        }
    }

    public Response get(@NonNull GetRequest request) {
        WebTarget target = client.target(request.getTarget());
        request.getQueryParams().forEach((k, v) -> target.queryParam(k, v));
        Invocation.Builder requestBuilder = target
                .request(request.getAcceptedResponse());
        request.getHeaders().forEach((k, v) -> requestBuilder.header(k, v));
        return requestBuilder.get();
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
