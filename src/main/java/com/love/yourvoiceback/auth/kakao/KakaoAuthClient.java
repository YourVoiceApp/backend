package com.love.yourvoiceback.auth.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoAuthClient {

    private final RestClient kakaoAuthRestClient;
    private final RestClient kakaoApiRestClient;
    private final String restApiKey;
    private final String redirectUri;

    public KakaoAuthClient(
            @Value("${auth.kakao.rest-api-key}") String restApiKey,
            @Value("${auth.kakao.redirect-uri}") String redirectUri
    ) {
        this.kakaoAuthRestClient = RestClient.builder()
                .baseUrl("https://kauth.kakao.com")
                .build();
        this.kakaoApiRestClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
        this.restApiKey = restApiKey;
        this.redirectUri = redirectUri;
    }

    public KakaoUserResponse getUserByAuthorizationCode(String code) {
        String accessToken = exchangeCodeForAccessToken(code);

        try {
            KakaoUserResponse response = kakaoApiRestClient.get()
                    .uri("/v2/user/me")
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null || response.providerUserId() == null) {
                throw new IllegalArgumentException("Invalid kakao user response");
            }
            if (!response.hasUsableEmail()) {
                throw new IllegalArgumentException("Kakao account email is required");
            }
            return response;
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Failed to fetch kakao user profile", ex);
        }
    }

    private String exchangeCodeForAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", restApiKey);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        try {
            KakaoTokenResponse response = kakaoAuthRestClient.post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new IllegalArgumentException("Failed to exchange kakao authorization code");
            }
            return response.accessToken();
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Failed to exchange kakao authorization code", ex);
        }
    }
}
