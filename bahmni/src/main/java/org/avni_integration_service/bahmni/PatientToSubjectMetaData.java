package org.avni_integration_service.bahmni;

public final class PatientToSubjectMetaData implements BahmniToAvniMetaData {
    private final String bahmniEntityUuidConcept;
    private final String avniIdentifierConcept;
    private final String patientEncounterType;
    private final String patientIdentifierName;
    private final MappingMetaDataCollection personAttributesMappingList;

    public PatientToSubjectMetaData(String bahmniEntityUuidConcept, String avniIdentifierConcept,
                                    String patientEncounterType, String patientIdentifierName,MappingMetaDataCollection personAttributesMappingList) {
        this.bahmniEntityUuidConcept = bahmniEntityUuidConcept;
        this.avniIdentifierConcept = avniIdentifierConcept;
        this.patientEncounterType = patientEncounterType;
        this.patientIdentifierName = patientIdentifierName;
        this.personAttributesMappingList = personAttributesMappingList;
    }

    @Override
    public String getBahmniEntityUuidConcept() {
        return bahmniEntityUuidConcept;
    }

    public String bahmniEntityUuidConcept() {
        return bahmniEntityUuidConcept;
    }

    public String avniIdentifierConcept() {
        return avniIdentifierConcept;
    }

    public String patientEncounterType() {
        return patientEncounterType;
    }

    public String patientIdentifierName() {
        return patientIdentifierName;
    }

    public MappingMetaDataCollection getPersonAttributesMappingList() {
        return personAttributesMappingList;
    }
}
