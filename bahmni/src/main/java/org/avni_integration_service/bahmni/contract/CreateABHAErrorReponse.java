package org.avni_integration_service.bahmni.contract;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateABHAErrorReponse {
    private String code;


    private String message;


    private List<ABHAErrorDetail> details;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ABHAErrorDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ABHAErrorDetail> details) {
        this.details = details;
    }
}
