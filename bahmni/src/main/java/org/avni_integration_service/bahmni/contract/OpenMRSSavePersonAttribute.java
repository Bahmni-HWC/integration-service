package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenMRSSavePersonAttribute {

    private OpenMRSSavePersonAttributeType attributeType;
    private String value;

    public OpenMRSSavePersonAttributeType getAttributeType() {
        return attributeType;
    }

    public OpenMRSSavePersonAttribute(OpenMRSSavePersonAttributeType attributeType, String value) {
        this.attributeType = attributeType;
        this.value = value;
    }

    public void setAttributeType(OpenMRSSavePersonAttributeType attributeType) {
        this.attributeType = attributeType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
