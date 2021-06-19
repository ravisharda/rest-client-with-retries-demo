package org.example.rs.http.retryingclient;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;

import javax.ws.rs.core.Response;
import java.net.ConnectException;
import java.time.Duration;
import java.util.function.Predicate;

public class RetryConfiguration {

    public static RetryConfig regularIntervalConfig(int maxAttempts, int waitDurationSeconds,
                                                    Predicate<Response> retryOnResult,
                                                    Predicate<Throwable> retryOnException) {
        RetryConfig.Builder<Response> resultBuilder = RetryConfig.<Response>custom();
        resultBuilder.maxAttempts(maxAttempts).waitDuration(Duration.ofSeconds(waitDurationSeconds));
        if (retryOnResult == null) {
            resultBuilder.retryOnResult(defaultRetryOnResponse());
        } else {
            resultBuilder.retryOnResult(retryOnResult);
        }
        if (retryOnException == null) {
            resultBuilder.retryOnException(defaultRetryOnException());
        } else {
            resultBuilder.retryOnException(retryOnException);
        }
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

    /**
     *
     * @param initialIntervalMillis the initial interval, i.e. the wait time for the first retry
     * @param multiplier
     * @param maxAttempts
     * @return
     */
    public static RetryConfig expBackoffConfig(long initialIntervalMillis,
                                               double multiplier,
                                               int maxAttempts,
                                               Predicate<Response> retryOnResult,
                                               Predicate<Throwable> retryOnException) {
        IntervalFunction intervalFn = IntervalFunction.ofExponentialBackoff(initialIntervalMillis,
                multiplier);
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(intervalFn)
                .build();
        return retryConfig;
    }
}
