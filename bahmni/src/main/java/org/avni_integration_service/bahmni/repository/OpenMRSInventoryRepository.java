package org.avni_integration_service.bahmni.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import org.avni_integration_service.bahmni.client.OpenMRSWebClient;
import org.avni_integration_service.bahmni.contract.OpenMRSInventoryDispenseRequest;
import org.avni_integration_service.bahmni.contract.OpenMRSUuidHolder;
import org.avni_integration_service.bahmni.contract.SearchResults;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenMRSInventoryRepository extends BaseOpenMRSRepository {

    private final String INVENTORY_BASE_PATH = "/openmrs/ws/rest/v2/inventory";

    @Autowired
    public OpenMRSInventoryRepository(OpenMRSWebClient openMRSWebClient) {
        super(openMRSWebClient);
    }

    public String getStockRoomUuidByName(String name) {
        String json = openMRSWebClient.get(String.format("%s/%s/stockroom?q=%s", getUrlPrefix(), INVENTORY_BASE_PATH, encode(name)));
        SearchResults<OpenMRSUuidHolder> searchResults = ObjectJsonMapper.readValue(json, new TypeReference<SearchResults<OpenMRSUuidHolder>>() {
        });
        return searchResults.getResults().get(0).getUuid();
    }

    public void performDispenseOperation(OpenMRSInventoryDispenseRequest openMRSInventoryDispenseRequest){
        String json = ObjectJsonMapper.writeValueAsString(openMRSInventoryDispenseRequest);
        String dispenseResponse = openMRSWebClient.post(String.format("%s%s/stockOperation",getUrlPrefix(),INVENTORY_BASE_PATH), json);
    }


}
