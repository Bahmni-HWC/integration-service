package org.avni_integration_service.goonj.repository;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.config.GoonjConfig;
import org.avni_integration_service.goonj.domain.DispatchReceiptConstants;
import org.avni_integration_service.goonj.domain.DispatchReceivedStatusLineItemConstants;
import org.avni_integration_service.goonj.dto.DispatchReceivedStatusLineItem;
import org.avni_integration_service.goonj.dto.DispatchReceivedStatusRequestDTO;
import org.avni_integration_service.goonj.dto.DispatchReceivedstatus;
import org.avni_integration_service.goonj.util.DateTimeUtil;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Component("DispatchReceiptRepository")
public class DispatchReceiptRepository extends GoonjBaseRepository
        implements DispatchReceiptConstants, DispatchReceivedStatusLineItemConstants {

    @Autowired
    public DispatchReceiptRepository(IntegratingEntityStatusRepository integratingEntityStatusRepository,
                                     @Qualifier("GoonjRestTemplate") RestTemplate restTemplate, GoonjConfig goonjConfig) {
        super(integratingEntityStatusRepository, restTemplate,
                goonjConfig, GoonjEntityType.DispatchReceipt.name());
    }
    @Override
    public HashMap<String, Object>[] fetchEvents() {
        throw new UnsupportedOperationException();
    }
    @Override
    public List<String> fetchDeletionEvents() {
        throw new UnsupportedOperationException();
    }
    @Override
    public HashMap<String, Object>[] createEvent(Subject subject, GeneralEncounter encounter) {
        DispatchReceivedStatusRequestDTO requestDTO = convertGeneralEncounterToDispatchReceivedStatusRequest(encounter);
        HttpEntity<DispatchReceivedStatusRequestDTO> request = new HttpEntity<>(requestDTO);
        return super.createSingleEntity(RESOURCE_DISPATCH_RECEIVED_STATUS, request);
    }
    private DispatchReceivedStatusRequestDTO convertGeneralEncounterToDispatchReceivedStatusRequest(GeneralEncounter encounter) {
        DispatchReceivedStatusRequestDTO requestDTO = new DispatchReceivedStatusRequestDTO();
        DispatchReceivedstatus drsDTO = new DispatchReceivedstatus();
        drsDTO.setSourceId(encounter.getUuid());
        drsDTO.setDispatchStatusId((String) encounter.getObservation(DISPATCH_STATUS_ID));
        Date dispatchReceivedDate = DateTimeUtil.convertToDate((String) encounter.getObservation(DISPATCH_RECEIVED_DATE));
        Calendar c = Calendar.getInstance();
        c.setTime(dispatchReceivedDate);
        c.add(Calendar.DATE, 1);
        dispatchReceivedDate = c.getTime();
        drsDTO.setReceivedDate(DateTimeUtil.formatDate(dispatchReceivedDate));
        drsDTO.setDispatchReceivedStatusLineItems(fetchDrsLineItemsFromEncounter(encounter));
        requestDTO.setDispatchReceivedStatus(Arrays.asList(drsDTO));
        return requestDTO;
    }
    private List<DispatchReceivedStatusLineItem> fetchDrsLineItemsFromEncounter(GeneralEncounter encounter) {
        ArrayList<HashMap<String, Object>> md = (ArrayList<HashMap<String, Object>>) encounter.getObservations().get(RECEIVED_MATERIAL);
        return md.stream().map(entry -> createDispatchReceivedStatusLineItem(entry)).collect(Collectors.toList());
    }

    public DispatchReceivedStatusLineItem createDispatchReceivedStatusLineItem(HashMap<String, Object> entry) {
        String dispatchStatusLineItemId = (String) entry.get(DISPATCH_STATUS_LINE_ITEM_ID);
        String typeOfMaterial = (String) entry.get(TYPE_OF_MATERIAL);
        String itemName = typeOfMaterial.equals(CONTRIBUTED_ITEM)?
                (String) entry.get(CONTRIBUTED_ITEM_NAME):
                (typeOfMaterial.equals(PURCHASED_ITEM) ? (String) entry.get(PURCHASED_ITEM_NAME) : (String) entry.get(KIT_NAME));
        String unit = (String) entry.get(UNIT);
        String receivingStatus = YES.equalsIgnoreCase((String) entry.get(QUANTITY_MATCHING)) ? RECIEVED_FULLY : RECIEVED_PARTIALLY;
        long dispatchedQuantity = ((Integer) entry.get(QUANTITY_DISPATCHED));
        long receivedQuantity = ((Integer) entry.get(QUANTITY));
        return new DispatchReceivedStatusLineItem(dispatchStatusLineItemId, typeOfMaterial, itemName,
                unit, receivingStatus, dispatchedQuantity, receivedQuantity);
    }
}