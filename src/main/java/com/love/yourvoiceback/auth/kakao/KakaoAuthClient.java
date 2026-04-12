package com.love.yourvoiceback.auth.kakao;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
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
                throw ApiException.error(ErrorCode.INVALID_KAKAO_USER_RESPONSE);
            }
            if (!response.hasUsableEmail()) {
                throw ApiException.error(ErrorCode.KAKAO_EMAIL_NOT_VERIFIED);
            }
            return response;
        } catch (RestClientException ex) {
            throw ApiException.error(ErrorCode.KAKAO_PROFILE_FETCH_FAILED, ex);
        }
    }
}
