package ewm.client;

import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class BaseClient {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.APPLICATION_JSON;
    private final RestTemplate restTemplate;

    public BaseClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static ResponseEntity<Object> prepareEwmResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());
        return response.hasBody() ? responseBuilder.body(response.getBody()) : responseBuilder.build();
    }

    protected ResponseEntity<Object> get(String path, @Nullable Map<String, Object> parameters) {
        return makeAndSendRequest(HttpMethod.GET, path, parameters, null);
    }

    protected <T> ResponseEntity<Object> post(String path, T body) {
        return makeAndSendRequest(HttpMethod.POST, path, null, body);
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(
            HttpMethod method,
            String path,
            @Nullable Map<String, Object> parameters,
            @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, createDefaultHeaders());

        ResponseEntity<Object> serverResponse;
        try {
            serverResponse = executeRequest(method, path, requestEntity, parameters);
        } catch (HttpStatusCodeException exception) {
            return handleHttpError(exception);
        }
        return prepareEwmResponse(serverResponse);
    }

    private <T> ResponseEntity<Object> executeRequest(
            HttpMethod method,
            String path,
            HttpEntity<T> requestEntity,
            @Nullable Map<String, Object> parameters) {
        if (parameters != null) {
            return restTemplate.exchange(path, method, requestEntity, Object.class, parameters);
        }
        return restTemplate.exchange(path, method, requestEntity, Object.class);
    }

    private ResponseEntity<Object> handleHttpError(HttpStatusCodeException exception) {
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(exception.getResponseBodyAsByteArray());
    }

    private HttpHeaders createDefaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(JSON_MEDIA_TYPE);
        headers.setAccept(List.of(JSON_MEDIA_TYPE));
        return headers;
    }
}