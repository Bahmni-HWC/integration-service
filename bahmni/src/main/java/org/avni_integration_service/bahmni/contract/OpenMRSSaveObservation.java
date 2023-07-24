package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.avni_integration_service.util.ObsDataType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenMRSSaveObservation {
    private String concept;
    private String obsDatetime;
    private Object value;
    private Object valueComplex;
    private String valueCodedName;
    private String uuid;
    private boolean voided;
    private String formFieldNamespace;
    private String formFieldPath;
    private List<OpenMRSSaveObservation> groupMembers;
    private static final String CHIEF_COMPLAINTS_CONCEPT_UUID = "9bb0795c-4ff0-0305-1990-000000000004";

    public OpenMRSSaveObservation() {
    }

    public static OpenMRSSaveObservation createVoidedObs(String uuid, String concept) {
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        openMRSSaveObservation.setUuid(uuid);
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.setVoided(true);
        return openMRSSaveObservation;
    }

    public static OpenMRSSaveObservation createVoidedObsWithMultipleGroupMember(String concept, String uuid, List<OpenMRSSaveObservation> groupMembers){
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        openMRSSaveObservation.setUuid(uuid);
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.groupMembers = groupMembers;
        openMRSSaveObservation.setVoided(true);
        return openMRSSaveObservation;
    }

    public static OpenMRSSaveObservation createPrimitiveObs(String concept, Object value, ObsDataType dataType) {
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.value = getValue(value, dataType);
        return openMRSSaveObservation;
    }

    public static OpenMRSSaveObservation createPrimitiveObsWithMultipleGroupMember(String concept, List<OpenMRSSaveObservation> groupMembers ) {
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        if(concept.equals(CHIEF_COMPLAINTS_CONCEPT_UUID)){
            openMRSSaveObservation.setFormFieldNamespace("avni");
            openMRSSaveObservation.setFormFieldPath("Chief Complaints");
        }
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.groupMembers = groupMembers;
        return openMRSSaveObservation;
    }

    public static OpenMRSSaveObservation createPrimitiveObs(String obsUuid, String concept, Object value, ObsDataType dataType) {
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        openMRSSaveObservation.uuid = obsUuid;
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.value = getValue(value, dataType);
        return openMRSSaveObservation;
    }

    private static Object getValue(Object value, ObsDataType dataType) {
        return ObsDataType.Date.equals(dataType)
                ? FormatAndParseUtil.fromAvniToOpenMRSDate(value.toString())
                : value;
    }

    public static OpenMRSSaveObservation createCodedObs(String concept, String valueUuid) {
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.value = valueUuid;
        return openMRSSaveObservation;
    }

    public static OpenMRSSaveObservation createCodedObs(String obsUuid, String concept, String valueUuid) {
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        openMRSSaveObservation.uuid = obsUuid;
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.value = valueUuid;
        return openMRSSaveObservation;
    }

    public static OpenMRSSaveObservation createComplexObs(String concept, String valueComplex){
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.valueComplex = valueComplex;
        return openMRSSaveObservation;
    }

    public static OpenMRSSaveObservation createComplexObs(String obsUuid, String concept, String valueComplex){
        OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
        openMRSSaveObservation.uuid = obsUuid;
        openMRSSaveObservation.concept = concept;
        openMRSSaveObservation.valueComplex = valueComplex;
        return openMRSSaveObservation;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getObsDatetime() {
        return obsDatetime;
    }

    public void setObsDatetime(String obsDatetime) {
        this.obsDatetime = obsDatetime;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getValueCodedName() {
        return valueCodedName;
    }

    public void setValueCodedName(String valueCodedName) {
        this.valueCodedName = valueCodedName;
    }

    public boolean isVoided() {
        return voided;
    }

    public void setVoided(boolean voided) {
        this.voided = voided;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<OpenMRSSaveObservation> getGroupMembers() {
        return groupMembers;
    }

    public void setGroupMembers(List<OpenMRSSaveObservation> groupMembers) {
        this.groupMembers = groupMembers;
    }

    public Object getValueComplex() {
        return valueComplex;
    }

    public void setValueComplex(Object valueComplex) {
        this.valueComplex = valueComplex;
    }

    public String getFormFieldNamespace() {
        return formFieldNamespace;
    }

    public void setFormFieldNamespace(String formFieldNamespace) {
        this.formFieldNamespace = formFieldNamespace;
    }

    public String getFormFieldPath() {
        return formFieldPath;
    }

    public void setFormFieldPath(String formFieldPath) {
        this.formFieldPath = formFieldPath;
    }
}
