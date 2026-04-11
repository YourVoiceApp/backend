package com.love.yourvoiceback.auth.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleTokenVerifier {

    private final RestClient restClient;
    private final String googleClientId;

    public GoogleTokenVerifier(
            @Value("${auth.google.client-id}") String googleClientId
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://oauth2.googleapis.com")
                .build();
        this.googleClientId = googleClientId;
    }

    public GoogleIdTokenPayload verify(String idToken) {
        GoogleIdTokenPayload payload = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/tokeninfo")
                        .queryParam("id_token", idToken)
                        .build())
                .retrieve()
                .body(GoogleIdTokenPayload.class);

        if (payload == null || payload.sub() == null || payload.email() == null) {
            throw new IllegalArgumentException("Invalid google id token payload");
        }
        if (!googleClientId.equals(payload.aud())) {
            throw new IllegalArgumentException("Google token audience mismatch");
        }
        if (!payload.isEmailVerified()) {
            throw new IllegalArgumentException("Google email is not verified");
        }
        return payload;
    }
}
