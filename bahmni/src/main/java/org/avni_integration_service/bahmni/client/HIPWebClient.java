package org.avni_integration_service.bahmni.client;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class HIPWebClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AvniHttpClient avniHttpClient;

    @Value("${hip.baseURL}")
    private String hipBaseURL;
    private static final Logger logger = Logger.getLogger(HIPWebClient.class);

    public ResponseEntity<String> post(String url, String body) {
        logger.info(String.format("HIP POST: %s", url));
        return getResponseEntity(String.class, getFullURI(url), HttpMethod.POST, body);
    }

    private <T> ResponseEntity<T> getResponseEntity(Class<T> returnType, URI uri, HttpMethod method, String body) {
        try {
            logger.debug("%s %s".formatted(method.name(), uri.toString()));
            return restTemplate.exchange(uri, method, new HttpEntity<>(body, getHeaders()), returnType);
        } catch (HttpClientErrorException.Unauthorized e) {
            avniHttpClient.clearAuthInformation();
            return restTemplate.exchange(uri, method, new HttpEntity<>(body, getHeaders()), returnType);
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + avniHttpClient.fetchAuthToken());
        headers.add("content-type", "application/json");
        return headers;
    }

    private URI getFullURI(String url) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(hipBaseURL + url);
        return builder.build().toUri();
    }
}
