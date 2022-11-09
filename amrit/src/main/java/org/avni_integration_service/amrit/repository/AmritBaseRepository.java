package org.avni_integration_service.amrit.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.amrit.config.AmritApplicationConfig;
import org.avni_integration_service.amrit.dto.AmritBaseResponse;
import org.avni_integration_service.amrit.dto.AmritUpsertBeneficiaryResponse;
import org.avni_integration_service.amrit.util.DateTimeUtil;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.*;

import java.net.URI;
import java.util.*;

public abstract class AmritBaseRepository {
    private static final Logger logger = Logger.getLogger(AmritBaseRepository.class);
    private static final String DELETION_RECORD_ID = "recordId";
    private static final String DELETION_SOURCE_ID = "sourceId";
    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;
    private final RestTemplate amritRestTemplate;
    protected final AmritApplicationConfig amritApplicationConfig;
    private final String entityType;

    public AmritBaseRepository(IntegratingEntityStatusRepository integratingEntityStatusRepository, RestTemplate restTemplate, AmritApplicationConfig amritApplicationConfig, String entityType) {
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
        this.amritRestTemplate = restTemplate;
        this.amritApplicationConfig = amritApplicationConfig;
        this.entityType = entityType;
    }


    protected <T> T getResponse(Date dateTime, String resource, Class<T> returnType) {
        URI uri = URI.create(String.format("%s/%s?dateTimestamp=%s", amritApplicationConfig.getAmritServerUrl(), resource, DateTimeUtil.formatDateTime(dateTime)));
        ResponseEntity<T> responseEntity = amritRestTemplate.exchange(uri, HttpMethod.GET, null, returnType);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to fetch data for resource %s, response status code is %s", resource, responseEntity.getStatusCode()));
        throw new HttpServerErrorException(responseEntity.getStatusCode());
    }

    /**
     * All our Sync time-stamps for Amrit, i.e. Beneficiary, CBAC Form, etc..
     * are stored using integrating_entity_status DB and none in avniEntityStatus table.
     *
     * @return cutOffDate
     */
    protected Date getCutOffDate() {
        return integratingEntityStatusRepository.findByEntityType(entityType).getReadUptoDateTime();
    }

    protected Date getCutOffDateTime() {
        return getCutOffDate();
    }

    protected <T> T getSingleEntityResponse(String resource, HttpEntity<?> requestEntity, Class<T> returnType) {
        URI uri = URI.create(String.format("%s/%s", amritApplicationConfig.getAmritServerUrl(), resource));
        ResponseEntity<T> responseEntity = amritRestTemplate.exchange(uri, HttpMethod.POST, requestEntity, returnType);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to fetch data for resource %s, response status code is %s", resource, responseEntity.getStatusCode()));
        throw getRestClientResponseException(responseEntity.getHeaders(), responseEntity.getStatusCode(), null);
    }

    protected <T extends AmritBaseResponse> T createSingleEntity(String resource, HttpEntity<?> requestEntity,Class<T> returnType) throws RestClientResponseException {
        logger.info("Request body:" + ObjectJsonMapper.writeValueAsString(requestEntity.getBody()));
        URI uri = URI.create(String.format("%s/%s", amritApplicationConfig.getAmritServerUrl(), resource));
        ResponseEntity<T> responseEntity = amritRestTemplate.exchange(uri, HttpMethod.POST, requestEntity, returnType);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to create resource %s,  response status code is %s", resource, responseEntity.getStatusCode()));
        throw handleError(responseEntity, responseEntity.getStatusCode());
    }

    protected Object deleteSingleEntity(String resource, HttpEntity<?> requestEntity) throws RestClientResponseException {
        logger.info("Request body:" + ObjectJsonMapper.writeValueAsString(requestEntity.getBody()));
        URI uri = URI.create(String.format("%s/%s", amritApplicationConfig.getAmritServerUrl(), resource));
        ParameterizedTypeReference<Object> responseType = new ParameterizedTypeReference<>() {
        };
        ResponseEntity<Object> responseEntity = amritRestTemplate.exchange(uri, HttpMethod.POST, requestEntity, responseType);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to delete resource %s, response error message is %s", resource, responseEntity.getBody()));
        throw getRestClientResponseException(responseEntity.getHeaders(), responseEntity.getStatusCode(), (String) responseEntity.getBody());
    }

    //todo modify error extraction logic based on Amrit api response
    protected RestClientException handleError(ResponseEntity<? extends AmritBaseResponse> responseEntity, HttpStatus statusCode) {
        AmritBaseResponse responseBody = responseEntity.getBody();
        return getRestClientResponseException(responseEntity.getHeaders(), statusCode, responseBody.getErrorMessage());
    }

    private RestClientResponseException getRestClientResponseException(HttpHeaders headers, HttpStatus statusCode, String message) {
        return switch (statusCode.series()) {
            case CLIENT_ERROR -> HttpClientErrorException.create(message, statusCode, null, headers, null, null);
            case SERVER_ERROR -> HttpServerErrorException.create(message, statusCode, null, headers, null, null);
            default -> new UnknownHttpStatusCodeException(message, statusCode.value(), null, headers, null, null);
        };
    }

    private HttpEntity<Map<String, List>> getDeleteEncounterHttpRequestEntity(GeneralEncounter encounter) {
        Map<String, List> deleteRequest = Map.of(DELETION_RECORD_ID, new ArrayList(), DELETION_SOURCE_ID, Arrays.asList(encounter.getUuid()));
        HttpEntity<Map<String, List>> requestEntity = new HttpEntity<>(deleteRequest);
        return requestEntity;
    }

    public abstract HashMap<String, Object>[] fetchEvents();

    public abstract <T extends AmritBaseResponse> T createEvent(Subject subject, GeneralEncounter encounter, Class<T> returnType);

    public boolean wasEventCreatedSuccessfully(HashMap<String, Object>[] response) {
        return (response != null && response[0].get("errorCode") == null);
    }

    public Object deleteEvent(String resourceType, GeneralEncounter encounter) {
        HttpEntity<Map<String, List>> requestEntity = getDeleteEncounterHttpRequestEntity(encounter);
        return deleteSingleEntity(resourceType, requestEntity);
    }

}
