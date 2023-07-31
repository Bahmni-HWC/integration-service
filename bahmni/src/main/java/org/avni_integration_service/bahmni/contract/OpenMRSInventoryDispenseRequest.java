package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenMRSInventoryDispenseRequest {
    private String status;
    private List<OpenMRSInventoryDispenseItem> items;
    @JsonProperty("patient")
    private String patientUuid;
    private String operationNumber;
    private String instanceType;
    private String source;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OpenMRSInventoryDispenseItem> getItems() {
        return items;
    }

    public void setItems(List<OpenMRSInventoryDispenseItem> items) {
        this.items = items;
    }

    public String getPatientUuid() {
        return patientUuid;
    }

    public void setPatientUuid(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    public String getOperationNumber() {
        return operationNumber;
    }

    public void setOperationNumber(String operationNumber) {
        this.operationNumber = operationNumber;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
