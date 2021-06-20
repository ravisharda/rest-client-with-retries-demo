package org.example.rs.http.retryingclient;

import lombok.Getter;

import javax.ws.rs.core.Response;

/**
 * This exception tells the client that the {@link Response} is not 200/OK.
 */
public class ResponseNotOkException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    @Getter
    private Response response;

    public ResponseNotOkException(Response response) {
        super(response.getStatusInfo().getReasonPhrase());
        this.response = response;
    }
}
