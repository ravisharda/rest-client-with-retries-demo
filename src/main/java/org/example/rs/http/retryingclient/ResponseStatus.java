package org.example.rs.http.retryingclient;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class ResponseStatus {

    private final static Set<Response.Status> nonRetryables = nonRetryableStatuses();

    /**
     * Checks whether the specified {@code statusType} represents a retryable HTTP error.
     *
     * @param statusType the response status
     * @return true if the error is retryable, else false
     */
    public static boolean isRetryableError(@NonNull Response.StatusType statusType) {
        boolean result = !nonRetryables.contains(statusType);
        log.debug("Returning retryable = {} for response status code {}",
                result, statusType.getStatusCode());
        return result;
    }

    /**
     * Returns a {@link Set} of non retryable HTTP {@link Response.Status}.
     *
     * @return a set of response statuses
     */
    private static Set<Response.Status> nonRetryableStatuses() {
        Set<Response.Status> result = new HashSet<>();

        result.add(Response.Status.OK);

        // Populate non-retryable 4XXs
        result.add(Response.Status.BAD_REQUEST);
        result.add(Response.Status.UNAUTHORIZED);
        result.add(Response.Status.FORBIDDEN);
        result.add(Response.Status.PAYMENT_REQUIRED);
        result.add(Response.Status.METHOD_NOT_ALLOWED);
        result.add(Response.Status.NOT_ACCEPTABLE);
        result.add(Response.Status.PROXY_AUTHENTICATION_REQUIRED);
        result.add(Response.Status.GONE);
        result.add(Response.Status.LENGTH_REQUIRED);
        result.add(Response.Status.PRECONDITION_FAILED);
        result.add(Response.Status.REQUEST_ENTITY_TOO_LARGE);
        result.add(Response.Status.REQUEST_URI_TOO_LONG);
        result.add(Response.Status.UNSUPPORTED_MEDIA_TYPE);
        result.add(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE);
        result.add(Response.Status.EXPECTATION_FAILED);
        result.add(Response.Status.REQUEST_HEADER_FIELDS_TOO_LARGE);

        // Populate non-retryable 5XXs
        // result.add(Response.Status.INTERNAL_SERVER_ERROR);
        result.add(Response.Status.NOT_IMPLEMENTED);
        result.add(Response.Status.HTTP_VERSION_NOT_SUPPORTED);
        result.add(Response.Status.NETWORK_AUTHENTICATION_REQUIRED);

        return result;
    }
}
