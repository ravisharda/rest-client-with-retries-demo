package org.example.rs.http.retryingclient;

import com.google.common.base.Preconditions;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class RetryConfigHelper {

    /**
     * Creates a {@link RetryConfig} with regular retry interval.
     *
     * @param maxAttempts the maximum number of attempts (the initial call is the first one)
     * @param waitDurationSeconds wait duration between two retry attempts
     * @param retryOnResult a predicate based on the {@link Response} returned for a request, returning true
     *                      if and only if the request is to be retried. If null, a default configuration is used.
     * @param retryOnException a predicate based on the {@link Throwable} thrown, returning true if and only
     *                         if the request is to be retried. If null, a default configuration is used.
     * @return a new {@link RetryConfig} instance
     */
    public static RetryConfig regularIntervalConfig(int maxAttempts, int waitDurationSeconds,
                                                    Predicate<Response> retryOnResult,
                                                    Predicate<Throwable> retryOnException) {
        Preconditions.checkArgument(maxAttempts >= 0, "negative value: %s", maxAttempts);
        Preconditions.checkArgument(waitDurationSeconds >= 0, "negative value: %s", waitDurationSeconds);

        RetryConfig.Builder<Response> resultBuilder = RetryConfig.<Response>custom();
        resultBuilder.maxAttempts(maxAttempts)
                .waitDuration(Duration.ofSeconds(waitDurationSeconds));

        resultBuilder.retryOnResult(retryOnResult != null ? retryOnResult : defaultRetryOnResponse());
        resultBuilder.retryOnException(retryOnResult != null ? retryOnException : defaultRetryOnException());
        resultBuilder.retryExceptions(IOException.class, TimeoutException.class);

        return resultBuilder.build();
    }

    /**
     * Creates a {@link RetryConfig} with exponential backoff.
     *
     * @param initialIntervalMillis the initial interval, i.e. the wait time for the first retry
     * @param maxAttempts the maximum number of attempts (the initial call is the first one)
     * @param multiplier
     * @param retryOnResult a predicate based on the {@link Response} returned for a request, returning true
     *                      if and only if the request is to be retried. If null, a default configuration is used.
     * @param retryOnException a predicate based on the {@link Throwable} thrown, returning true if and only
     *                         if the request is to be retried. If null, a default configuration is used.
     * @return a new {@link RetryConfig} instance
     */
    public static RetryConfig expBackoffConfig(long initialIntervalMillis,
                                               double multiplier,
                                               int maxAttempts,
                                               Predicate<Response> retryOnResult,
                                               Predicate<Throwable> retryOnException) {
        Preconditions.checkArgument(initialIntervalMillis >= 0, "negative value: %s", initialIntervalMillis);
        Preconditions.checkArgument(multiplier >= 0.0, "negative value: %s", multiplier);
        Preconditions.checkArgument(multiplier >= 0.0, "negative value: %s", maxAttempts);

        IntervalFunction intervalFn = IntervalFunction.ofExponentialBackoff(initialIntervalMillis,
                multiplier);
        RetryConfig.Builder<Response> resultBuilder = RetryConfig.<Response>custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(intervalFn);

        resultBuilder.retryOnResult(retryOnResult != null ? retryOnResult : defaultRetryOnResponse());
        resultBuilder.retryOnException(retryOnResult != null ? retryOnException : defaultRetryOnException());
        resultBuilder.retryExceptions(IOException.class, TimeoutException.class);

        return resultBuilder.build();
    }

    /**
     * Returns the condition for which retry will happen for an HTTP response.
     *
     * @return
     */
    static Predicate<Response> defaultRetryOnResponse() {
        return response -> !response.getStatusInfo().equals(Response.Status.OK)
                        && ResponseStatus.isRetryableError(response.getStatusInfo());
    }

    /**
     * Returns the condition for which retry will happen when exceptions occur.
     *
     * @return
     */
    static Predicate<Throwable> defaultRetryOnException() {
        return e -> e.getCause() instanceof ConnectException;
    }
}
