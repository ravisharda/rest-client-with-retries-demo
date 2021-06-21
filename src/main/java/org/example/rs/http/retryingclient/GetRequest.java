package org.example.rs.http.retryingclient;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@Builder
@ToString
public class GetRequest {
    @NonNull
    @Getter
    private String uri;

    @Builder.Default
    @Getter
    private String acceptedResponse = "application/json";

    @Builder.Default
    @Getter
    private Map<String, Object> headers = new HashMap<>();

    @Builder.Default
    @Getter
    private Map<String, String> queryParams = new HashMap<>();
}
