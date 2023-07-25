package org.avni_integration_service.bahmni;

public final class SubjectToPatientMetaData {
    private final String avniIdentifierConcept;
    private final String encounterTypeUuid;
    private final String subjectUuidConceptUuid;
    private final MappingMetaDataCollection personAttributesMappingList;

    public SubjectToPatientMetaData(String avniIdentifierConcept, String encounterTypeUuid, String subjectUuidConceptUuid, MappingMetaDataCollection personAttributesMappingList) {
        this.avniIdentifierConcept = avniIdentifierConcept;
        this.encounterTypeUuid = encounterTypeUuid;
        this.subjectUuidConceptUuid = subjectUuidConceptUuid;
        this.personAttributesMappingList = personAttributesMappingList;
    }

    public String avniIdentifierConcept() {
        return avniIdentifierConcept;
    }

    public String encounterTypeUuid() {
        return encounterTypeUuid;
    }

    public String subjectUuidConceptUuid() {
        return subjectUuidConceptUuid;
    }
    public MappingMetaDataCollection getPersonAttributesMappingList() {
        return personAttributesMappingList;
    }
}
