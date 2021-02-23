package org.bahmni_avni_integration.contract.avni;

import org.bahmni_avni_integration.contract.internal.SubjectToPatientMetaData;

public class Subject extends AvniBaseContract {

    public String getId(SubjectToPatientMetaData subjectToPatientMetaData) {
        return (String) getObservation(subjectToPatientMetaData.avniIdentifierConcept());
    }
}