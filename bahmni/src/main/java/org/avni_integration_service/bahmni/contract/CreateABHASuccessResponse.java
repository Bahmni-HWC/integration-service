package org.avni_integration_service.bahmni.contract;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateABHASuccessResponse {
    private String healthId;
    private String healthIdNumber;

    public String getHealthId() {
        return healthId;
    }

    public void setHealthId(String healthId) {
        this.healthId = healthId;
    }

    public String getHealthIdNumber() {
        return healthIdNumber;
    }

    public void setHealthIdNumber(String healthIdNumber) {
        this.healthIdNumber = healthIdNumber;
    }
}
