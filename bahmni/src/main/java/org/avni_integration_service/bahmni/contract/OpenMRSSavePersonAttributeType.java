package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMRSSavePersonAttributeType {
    private String uuid;
    private String display;

    public OpenMRSSavePersonAttributeType(String uuid, String display) {
        this.uuid = uuid;
        this.display = display;
    }

    public OpenMRSSavePersonAttributeType() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

}

