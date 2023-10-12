package org.avni_integration_service.avni.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.avni_integration_service.util.MapUtil;

import java.util.Date;
import java.util.List;

public class Subject extends AvniBaseContract {
    public static final String SubjectTypeFieldName = "Subject type";
    public static final String AddressFieldName = "Address";
    public static final String ExternalIdFieldName = "External ID";
    public static final String FirstNameFieldName = "First name";
    public static final String MiddleNameFieldName = "Middle name";
    public static final String LastNameFieldName = "Last name";
    public static final String GenderFieldName = "Gender";
    public static final String DobFieldName = "Date of birth";

    public static final String RegistrationDateFieldName = "Registration date";

    public static final String CatchmentsFieldName = "Catchments";

    public String getId(String avniIdentifierConcept) {
        return (String) getObservation(avniIdentifierConcept);
    }

    @JsonIgnore
    public String getFirstName() {
        return (String) getObservation(FirstNameFieldName);
    }

    public void setFirstName(String firstName) {
        set(FirstNameFieldName, firstName);
    }

    @JsonIgnore
    public String getLastName() {
        return (String) getObservation("Last name");
    }

    public void setLastName(String lastName) {
        set(LastNameFieldName, lastName);
    }

    @JsonIgnore
    public String getMiddleName() {
        return (String) getObservation("Middle name");
    }

    public void setMiddleName(String middleName) {
        set(MiddleNameFieldName, middleName);
    }

    @JsonIgnore
    public String getGender() {
        return (String) getObservation("Gender");
    }

    public void setGender(String gender) {
        set(GenderFieldName, gender);
    }

    @JsonIgnore
    public String getDob() {
        return (String) getObservation("Date of birth");
    }

    public void setDob(String dob) {
        set(DobFieldName, dob);
    }

    @JsonIgnore
    public String getDateOfBirth() {
        return (String) getObservation("Date of birth");
    }

    public void setRegistrationDate(Date date) {
        set(RegistrationDateFieldName, FormatAndParseUtil.toISODateString(date));
    }

    @JsonIgnore
    public Date getRegistrationDate() {
        var registrationDate = (String) map.get(RegistrationDateFieldName);
        return registrationDate == null ? null : FormatAndParseUtil.fromAvniDate(registrationDate);
    }

    @JsonIgnore
    public boolean isCompleted() {
        return getRegistrationDate() != null;
    }

    public void setSubjectType(String subjectTYpe) {
        this.set(Subject.SubjectTypeFieldName, subjectTYpe);
    }

    public void setExternalId(String externalId) {
        map.put(ExternalIdFieldName, externalId);
    }

    @JsonIgnore
    public String getExternalId() {
        return MapUtil.getString(ExternalIdFieldName, this.map);
    }

    public void setAddress(String address) {
        map.put(AddressFieldName, address);
    }

    @JsonIgnore
    public String getAddress() {
        return MapUtil.getString(AddressFieldName, this.map);
    }

    public void setCatchments(List<String> catchments) {
        map.put(CatchmentsFieldName, catchments);
    }

    public List<String> getCatchments(){
        Object catchments = map.get(CatchmentsFieldName);
        return catchments == null ? null : (List<String>) catchments;
    }
    @JsonIgnore
    public String getFullName() {
        if (getMiddleName() == null || getMiddleName().isEmpty()) {
            return (getFirstName() + " " + getLastName()).trim();
        } else {
            return (getFirstName() + " " + getMiddleName() + " " + getLastName()).trim();
        }
    }
}
