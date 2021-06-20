package org.example.rs.http.retryingclient;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder
@ToString
public class GetRequest {
    @NonNull
    @Getter
    private String uri;

    @Builder.Default
    @Getter
    private String acceptedResponse = "application/json";
}
