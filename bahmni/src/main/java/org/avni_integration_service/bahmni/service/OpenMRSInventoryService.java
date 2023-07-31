package org.avni_integration_service.bahmni.service;

import org.avni_integration_service.bahmni.contract.OpenMRSInventoryDispenseRequest;
import org.avni_integration_service.bahmni.repository.OpenMRSInventoryRepository;
import org.springframework.stereotype.Service;

@Service
public class OpenMRSInventoryService {

    private final OpenMRSInventoryRepository openMRSInventoryRepository;

    public OpenMRSInventoryService(OpenMRSInventoryRepository openMRSInventoryRepository) {
        this.openMRSInventoryRepository = openMRSInventoryRepository;
    }

    public String getStockRoomUuidForCatchment(String catchmentName){
        return openMRSInventoryRepository.getStockRoomUuidByName(catchmentName);
    }

    public void dispense(OpenMRSInventoryDispenseRequest openMRSInventoryDispenseRequest){
        openMRSInventoryRepository.performDispenseOperation(openMRSInventoryDispenseRequest);
    }
}
