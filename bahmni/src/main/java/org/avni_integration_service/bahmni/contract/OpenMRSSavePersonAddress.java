package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenMRSSavePersonAddress {

    private String cityVillage;
    private String stateProvince;
    private String countyDistrict;
    private String address4; //subdistrict

    public String getCityVillage() {
        return cityVillage;
    }
    public void setCityVillage(String cityVillage) {
        this.cityVillage = cityVillage;
    }

    public String getStateProvince() {
        return stateProvince;
    }

    public void setStateProvince(String stateProvince) {
        this.stateProvince = stateProvince;
    }

    public String getCountyDistrict() {
        return countyDistrict;
    }

    public void setCountyDistrict( String countyDistrict ) {
        this.countyDistrict = countyDistrict;
    }

    public String getAddress4() {
        return address4;
    }

    public void setAddress4( String address4 ) {
        this.address4 = address4;
    }
}