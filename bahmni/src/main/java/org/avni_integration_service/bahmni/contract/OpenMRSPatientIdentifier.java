package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMRSPatientIdentifier {
    private String identifier;
    private boolean preferred;
    private OpenMRSPatientIdentifierType identifierType;

    public OpenMRSPatientIdentifier() {
    }

    public OpenMRSPatientIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isPreferred() {
        return preferred;
    }

    public void setPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    public OpenMRSPatientIdentifierType getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(OpenMRSPatientIdentifierType identifierType) {
        this.identifierType = identifierType;
    }
}
