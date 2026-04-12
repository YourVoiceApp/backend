package com.love.yourvoiceback.auth.kakao;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoAuthClient {

    private final RestClient kakaoApiRestClient;

    public KakaoAuthClient() {
        this.kakaoApiRestClient = RestClient.builder()
                .baseUrl("https://kapi.kakao.com")
                .build();
    }

    public KakaoUserResponse getUserByAccessToken(String accessToken) {
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
}
