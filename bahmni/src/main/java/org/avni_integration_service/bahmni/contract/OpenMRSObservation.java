package org.avni_integration_service.bahmni.contract;

import java.util.List;

public class OpenMRSObservation {
    private String conceptUuid;
    private Object value;
    private Object valueComplex;
    private String obsUuid;
    private boolean voided;
    private List<OpenMRSObservation> groupMembers;

    public String getConceptUuid() {
        return conceptUuid;
    }

    public void setConceptUuid(String uuid) {
        this.conceptUuid = uuid;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getObsUuid() {
        return obsUuid;
    }

    public void setObsUuid(String obsUuid) {
        this.obsUuid = obsUuid;
    }

    @Override
    public String toString() {
        return "{" +
                "conceptUuid='" + conceptUuid + '\'' +
                ", value=" + value +
                '}';
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public boolean isVoided() {
        return voided;
    }

    public Object getValueComplex() {
        return valueComplex;
    }

    public void setValueComplex(Object valueComplex) {
        this.valueComplex = valueComplex;
    }

    public List<OpenMRSObservation> getGroupMembers() {
        return groupMembers;
    }

    public void setGroupMembers(List<OpenMRSObservation> groupMembers) {
        this.groupMembers = groupMembers;
    }
}
