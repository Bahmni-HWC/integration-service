package org.avni_integration_service.bahmni.mapper;

import org.avni_integration_service.avni.repository.AvniConceptRepository;
import org.avni_integration_service.bahmni.contract.OpenMRSInventoryDispenseItem;
import org.avni_integration_service.bahmni.contract.OpenMRSInventoryDispenseRequest;
import org.avni_integration_service.bahmni.service.OpenMRSInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.avni_integration_service.bahmni.constants.DispenseConstants.*;

@Component
public class DispenseRequestMapper {

    @Autowired
    private OpenMRSInventoryService openMRSInventoryService;

    @Autowired
    private AvniConceptRepository avniConceptRepository;

    public OpenMRSInventoryDispenseRequest mapToOpenMRSInventoryDispenseRequest(String patientUuid, String catchmentName, List<Map<String, Object>> avniDispenseObs) {
        OpenMRSInventoryDispenseRequest openMRSInventoryDispenseRequest = new OpenMRSInventoryDispenseRequest();
        setConstants(openMRSInventoryDispenseRequest);
        openMRSInventoryDispenseRequest.setPatientUuid(patientUuid);
        openMRSInventoryDispenseRequest.setSource(openMRSInventoryService.getStockRoomUuidForCatchment(catchmentName));
        openMRSInventoryDispenseRequest.setItems(mapItems(avniDispenseObs));
        return openMRSInventoryDispenseRequest;
    }

    private void setConstants(OpenMRSInventoryDispenseRequest openMRSInventoryDispenseRequest) {
        openMRSInventoryDispenseRequest.setOperationNumber("WILL BE GENERATED");
        openMRSInventoryDispenseRequest.setStatus("NEW");
        openMRSInventoryDispenseRequest.setInstanceType(DISPENSE_STOCK_OPERATION_UUID);

    }

    private List<OpenMRSInventoryDispenseItem> mapItems(List<Map<String, Object>> avniDispenseObs) {
        List<OpenMRSInventoryDispenseItem> openMRSInventoryDispenseItems = new ArrayList<>();
        avniDispenseObs.forEach(avniDispenseOb -> {
            OpenMRSInventoryDispenseItem openMRSInventoryDispenseItem = new OpenMRSInventoryDispenseItem();
            openMRSInventoryDispenseItem.setItemUuid(getInventoryItemUuid((String) avniDispenseOb.get(AVNI_DRUG_NAME_FIELD)));
            openMRSInventoryDispenseItem.setQuantity((int) avniDispenseOb.get(AVNI_DRUG_QUANTITY_FIELD));
            openMRSInventoryDispenseItem.setCalculatedExpiration(true);
            openMRSInventoryDispenseItems.add(openMRSInventoryDispenseItem);
        });

        return openMRSInventoryDispenseItems;

    }

    private String getInventoryItemUuid(String drugName) {
        return avniConceptRepository.getConceptByName(drugName).getUuid();
    }

}
