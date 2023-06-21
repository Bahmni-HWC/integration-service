package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenMRSSavePerson {
    private List<OpenMRSSaveName> names;
    private String gender;

    private List<OpenMRSSavePersonAddress> addresses;

    @JsonProperty("birthdate")
    private String birthDate;
    
    private List<OpenMRSSavePersonAttribute> attributes;

    public List<OpenMRSSaveName> getNames() {
        return names;
    }

    public void setNames(List<OpenMRSSaveName> names) {
        this.names = names;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public List<OpenMRSSavePersonAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<OpenMRSSavePersonAddress> addresses) {
        this.addresses = addresses;
    }

    public void setAttributes(List<OpenMRSSavePersonAttribute> attributes) {
        this.attributes = attributes;
    }

    public List<OpenMRSSavePersonAttribute> getAttributes() {
        return attributes;
    }
}
