package com.love.yourvoiceback.external.supertone;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.external.supertone.dto.SupertoneCreateClonedVoiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class SupertoneVoiceClient {

    private final RestClient restClient;
    private final String apiKey;

    public SupertoneVoiceClient(
            @Value("${supertone.api.base-url}") String baseUrl,
            @Value("${supertone.api.key:}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    public SupertoneCreateClonedVoiceResponse createClonedVoice(
            MultipartFile file,
            String name,
            String description
    ) {
        if (!StringUtils.hasText(apiKey)) {
            throw ApiException.error(ErrorCode.SUPERTONE_UNAUTHORIZED, "Supertone API key is not configured");
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("files", new MultipartInputFile(file));
            body.add("name", name);
            if (StringUtils.hasText(description)) {
                body.add("description", description);
            }

            SupertoneCreateClonedVoiceResponse response = restClient.post()
                    .uri("/v1/custom-voices/cloned-voice")
                    .header("x-sup-api-key", apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(SupertoneCreateClonedVoiceResponse.class);

            if (response == null || !StringUtils.hasText(response.voiceId())) {
                throw ApiException.error(ErrorCode.SUPERTONE_REQUEST_FAILED, "Supertone response did not include voice_id");
            }

            return response;
        } catch (RestClientResponseException ex) {
            throw mapSupertoneException(ex);
        } catch (RestClientException ex) {
            throw ApiException.error(
                    ErrorCode.SUPERTONE_REQUEST_FAILED,
                    "Failed to call Supertone API: " + ex.getMessage()
            );
        }
    }

    private ApiException mapSupertoneException(RestClientResponseException ex) {
        int statusCode = ex.getStatusCode().value();
        return switch (statusCode) {
            case 400 -> ApiException.error(ErrorCode.SUPERTONE_BAD_REQUEST, extractMessage(ex, ErrorCode.SUPERTONE_BAD_REQUEST.message()));
            case 401 -> ApiException.error(ErrorCode.SUPERTONE_UNAUTHORIZED, extractMessage(ex, ErrorCode.SUPERTONE_UNAUTHORIZED.message()));
            case 403 -> ApiException.error(ErrorCode.SUPERTONE_FORBIDDEN, extractMessage(ex, ErrorCode.SUPERTONE_FORBIDDEN.message()));
            case 413 -> ApiException.error(ErrorCode.SUPERTONE_FILE_TOO_LARGE, extractMessage(ex, ErrorCode.SUPERTONE_FILE_TOO_LARGE.message()));
            case 415 -> ApiException.error(ErrorCode.SUPERTONE_UNSUPPORTED_MEDIA_TYPE, extractMessage(ex, ErrorCode.SUPERTONE_UNSUPPORTED_MEDIA_TYPE.message()));
            case 429 -> ApiException.error(ErrorCode.SUPERTONE_RATE_LIMITED, extractMessage(ex, ErrorCode.SUPERTONE_RATE_LIMITED.message()));
            default -> ApiException.error(ErrorCode.SUPERTONE_REQUEST_FAILED, extractMessage(ex, ErrorCode.SUPERTONE_REQUEST_FAILED.message()));
        };
    }

    private String extractMessage(RestClientResponseException ex, String defaultMessage) {
        String responseBody = ex.getResponseBodyAsString();
        if (StringUtils.hasText(responseBody)) {
            return responseBody;
        }
        return defaultMessage;
    }

    private static final class MultipartInputFile extends ByteArrayResource {
        private final String filename;

        private MultipartInputFile(MultipartFile file) {
            super(readBytes(file));
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return filename;
        }

        private static byte[] readBytes(MultipartFile file) {
            try {
                return file.getBytes();
            } catch (IOException ex) {
                throw ApiException.error(ErrorCode.INVALID_REQUEST, "Failed to read uploaded audio file");
            }
        }
    }
}
