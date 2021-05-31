package org.bahmni_avni_integration.contract.bahmni;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMRSVisitAttribute {
    private OpenMRSUuidHolder attributeType;
    private String value;

    public OpenMRSUuidHolder getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(OpenMRSUuidHolder attributeType) {
        this.attributeType = attributeType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}