package org.avni_integration_service.avni.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.AvniConcept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class AvniConceptRepository extends BaseAvniRepository {
    @Autowired
    private AvniHttpClient avniHttpClient;

    public AvniConcept getConceptByName(String conceptName) {
        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("name", conceptName);
        ResponseEntity<AvniConcept> avniConceptResponseEntity = avniHttpClient.get("/web/concept", queryParams, AvniConcept.class);
        if(avniConceptResponseEntity.getStatusCode().is2xxSuccessful())
            return avniConceptResponseEntity.getBody();
        else
            return null;
    }
}
