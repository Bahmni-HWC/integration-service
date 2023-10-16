package org.avni_integration_service.bahmni.service;

import org.apache.http.HttpStatus;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.BahmniEntityType;
import org.avni_integration_service.bahmni.BahmniErrorType;
import org.avni_integration_service.bahmni.ConstantKey;
import org.avni_integration_service.bahmni.SubjectToPatientMetaData;
import org.avni_integration_service.bahmni.client.WebClientsException;
import org.avni_integration_service.bahmni.contract.*;
import org.avni_integration_service.bahmni.mapper.avni.SubjectMapper;
import org.avni_integration_service.bahmni.repository.OpenMRSEncounterRepository;
import org.avni_integration_service.bahmni.repository.OpenMRSPatientRepository;
import org.avni_integration_service.bahmni.repository.openmrs.OpenMRSPersonRepository;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEncounterEventWorker;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.ict4h.atomfeed.client.domain.Event;
import org.javatuples.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PatientService {
    private final SubjectMapper subjectMapper;
    private final OpenMRSEncounterRepository openMRSEncounterRepository;
    private final OpenMRSPatientRepository openMRSPatientRepository;
    private final AvniBahmniErrorService avniBahmniErrorService;
    private final OpenMRSPersonRepository openMRSPersonRepository;
    private final VisitService visitService;

    public PatientService(SubjectMapper subjectMapper, OpenMRSEncounterRepository openMRSEncounterRepository, OpenMRSPatientRepository openMRSPatientRepository, AvniBahmniErrorService avniBahmniErrorService, OpenMRSPersonRepository openMRSPersonRepository, VisitService visitService) {
        this.subjectMapper = subjectMapper;
        this.openMRSEncounterRepository = openMRSEncounterRepository;
        this.openMRSPatientRepository = openMRSPatientRepository;
        this.avniBahmniErrorService = avniBahmniErrorService;
        this.openMRSPersonRepository = openMRSPersonRepository;
        this.visitService = visitService;
    }

    public void updateSubject(OpenMRSFullEncounter existingEncounter, OpenMRSPatient patient, Subject subject, SubjectToPatientMetaData subjectToPatientMetaData, Constants constants) {
        if (subject.getVoided()) {
            openMRSEncounterRepository.voidEncounter(existingEncounter);
        } else {
            OpenMRSEncounter encounter = subjectMapper.mapSubjectToExistingEncounter(existingEncounter, subject, patient.getUuid(), subjectToPatientMetaData.encounterTypeUuid(), constants);
            openMRSEncounterRepository.updateEncounter(encounter);
            avniBahmniErrorService.successfullyProcessed(subject);
        }
    }

    public OpenMRSFullEncounter createSubject(Subject subject, OpenMRSPatient patient, SubjectToPatientMetaData subjectToPatientMetaData, Constants constants) {
        if (subject.getVoided())
            return null;

        var visit = visitService.getOrCreateVisit(patient, subject, subject.getRegistrationDate());
        OpenMRSEncounter encounter = subjectMapper.mapSubjectToEncounter(subject, patient.getUuid(), subjectToPatientMetaData.encounterTypeUuid(), constants, visit);
        OpenMRSFullEncounter savedEncounter = openMRSEncounterRepository.createEncounter(encounter);

        avniBahmniErrorService.successfullyProcessed(subject);
        return savedEncounter;
    }

    public OpenMRSFullEncounter createPatientAndSubject(Subject subject, SubjectToPatientMetaData subjectToPatientMetaData, Constants constants) {
        if (subject.getVoided())
            return null;

        var newPatient = createPatient(subject, subjectToPatientMetaData, constants);
        var fullPatientObject = getPatient(newPatient.getUuid());
        return createSubject(subject, fullPatientObject, subjectToPatientMetaData, constants);
    }

    public Pair<OpenMRSPatient, OpenMRSFullEncounter> findSubject(Subject subject, Constants constants, SubjectToPatientMetaData subjectToPatientMetaData) throws PatientEncounterEventWorker.SubjectIdChangedException {
        String subjectId = subject.getUuid();
        OpenMRSPatient patient = findPatient(subject, constants, subjectToPatientMetaData);
        if (patient == null) {
            return new Pair<>(null, null);
        }

        List<OpenMRSFullEncounter> encounters = openMRSEncounterRepository.getEncounterByPatientAndEncType(patient.getUuid(), subjectToPatientMetaData.encounterTypeUuid());
        OpenMRSFullEncounter encounter = encounters.stream().filter(openMRSFullEncounter -> subjectId.equals(openMRSFullEncounter.getObservationValue(subjectToPatientMetaData.subjectUuidConceptUuid()))).findFirst().orElse(null);
        if (encounters.size() > 0 && encounter == null)
            throw new PatientEncounterEventWorker.SubjectIdChangedException();
        return new Pair<>(patient, encounter);
    }

    public OpenMRSPatient findPatient(Subject subject, Constants constants, SubjectToPatientMetaData subjectToPatientMetaData) {
        String patientIdentifier =  subject.getId(subjectToPatientMetaData.avniIdentifierConcept());
        return openMRSPatientRepository.getPatientByIdentifier(patientIdentifier);
    }

    public void processPatientIdChanged(Subject subject, SubjectToPatientMetaData metaData) {
        avniBahmniErrorService.errorOccurred(subject, BahmniErrorType.PatientIdChanged);
    }

    private OpenMRSPatient createPatient(Subject subject, SubjectToPatientMetaData metaData, Constants constants) {
        OpenMRSSavePerson person = new OpenMRSSavePerson();
        person.setNames(List.of(new OpenMRSSaveName(
                subject.getFirstName(),
                subject.getLastName(),
                true
        )));
        person.setBirthDate(subject.getDateOfBirth());
        person.setGender(FormatAndParseUtil.fromAvniToOpenMRSGender((String) subject.getObservation("Gender")));

        OpenMRSSavePersonAddress personAddress = new OpenMRSSavePersonAddress();

        personAddress.setCityVillage((String) subject.getLocation("City/Village"));
        personAddress.setAddress4((String) subject.getLocation("Sub District"));
        personAddress.setCountyDistrict((String) subject.getLocation("District"));
        personAddress.setStateProvince((String) subject.getLocation("State"));

        person.setAddresses(List.of(personAddress));

        List<OpenMRSSavePersonAttribute> attributes = new ArrayList<>();

        String phoneNumberUuid = metaData.getPersonAttributesMappingList().getBahmniValueForAvniValue("Phone Number");
        if (subject.getObservation("Phone Number") != null && phoneNumberUuid != null){
            OpenMRSSavePersonAttributeType phoneNumberAttributeType = new OpenMRSSavePersonAttributeType(phoneNumberUuid, "phoneNumber");
            OpenMRSSavePersonAttribute phoneNumberAttribute = new OpenMRSSavePersonAttribute(phoneNumberAttributeType, (String) subject.getObservation("Phone Number"));
            attributes.add(phoneNumberAttribute);
        }

        String fatherOrMotherNameUuid = metaData.getPersonAttributesMappingList().getBahmniValueForAvniValue("Father/Mother's Name");
        if (subject.getObservation("Father/Mother's Name") != null && fatherOrMotherNameUuid != null) {
            OpenMRSSavePersonAttributeType fatherOrMotherNameAttributeType = new OpenMRSSavePersonAttributeType(fatherOrMotherNameUuid, "father/motherName");
            OpenMRSSavePersonAttribute fatherOrMotherNameAttribute = new OpenMRSSavePersonAttribute(fatherOrMotherNameAttributeType, (String) subject.getObservation("Father/Mother's Name"));
            attributes.add(fatherOrMotherNameAttribute);
        }

        person.setAttributes(attributes);

        OpenMRSUuidHolder uuidHolder = openMRSPersonRepository.createPerson(person);
        OpenMRSSavePatient patient = new OpenMRSSavePatient();
        patient.setPerson(uuidHolder.getUuid());

        List<OpenMRSSavePatientIdentifier> identifiers = new ArrayList<>();
        identifiers.add(new OpenMRSSavePatientIdentifier(
                subject.getId(metaData.avniIdentifierConcept()),
                constants.getValue(ConstantKey.IntegrationBahmniIdentifierType.name()),
                true
        ));
        identifiers.addAll(mapExtraIdentifiers(subject, metaData, constants));
        patient.setIdentifiers(identifiers);

        return openMRSPatientRepository.createPatient(patient);
    }

    private List<OpenMRSSavePatientIdentifier> mapExtraIdentifiers(Subject subject, SubjectToPatientMetaData metaData, Constants constants) {

        List<OpenMRSSavePatientIdentifier> identifiers = new ArrayList<>();
        String rchIdUuid = metaData.getPersonAttributesMappingList().getBahmniValueForAvniValue("RCH ID");
        if (subject.getObservation("RCH ID") != null && rchIdUuid != null) {
            identifiers.add(new OpenMRSSavePatientIdentifier(
                    subject.getObservation("RCH ID").toString(), rchIdUuid, false
            ));
        }

        String nikshayIdUuid = metaData.getPersonAttributesMappingList().getBahmniValueForAvniValue("Nikshay ID");
        if (subject.getObservation("Nikshay ID") != null && nikshayIdUuid != null) {
            identifiers.add(new OpenMRSSavePatientIdentifier(
                    subject.getObservation("Nikshay ID").toString(), nikshayIdUuid, false
            ));
        }

        String abhaAddressUuid = metaData.getPersonAttributesMappingList().getBahmniValueForAvniValue("ABHA Address");
        if (subject.getObservation("ABHA Address") != null && abhaAddressUuid != null) {
            identifiers.add(new OpenMRSSavePatientIdentifier(
                    subject.getObservation("ABHA Address").toString(), abhaAddressUuid, false
            ));
        }

        String abhaNumberUuid = metaData.getPersonAttributesMappingList().getBahmniValueForAvniValue("ABHA Number");
        if (subject.getObservation("ABHA Number") != null && abhaAddressUuid != null) {
            identifiers.add(new OpenMRSSavePatientIdentifier(
                    subject.getObservation("ABHA Number").toString(), abhaNumberUuid, false
            ));
        }
        return identifiers;

    }

    //    doesn't work
    public void deleteSubject(OpenMRSBaseEncounter encounter) {
        openMRSEncounterRepository.deleteEncounter(encounter);
    }

    public OpenMRSPatient getPatient(Event event) {
        try {
            return openMRSPatientRepository.getPatient(event);
        } catch (WebClientsException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    public boolean shouldFilterPatient(OpenMRSPatient patient, Constants constants) {
        String patientId = patient.getPatientId();
        return !patientId.startsWith(constants.getValue(ConstantKey.BahmniIdentifierPrefix.name()));
    }

    public OpenMRSPatient getPatient(String patientUuid) {
        try {
            return openMRSPatientRepository.getPatient(patientUuid);
        } catch (WebClientsException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    public void patientDeleted(String patientUuid) {
        avniBahmniErrorService.errorOccurred(patientUuid, BahmniErrorType.EntityIsDeleted, BahmniEntityType.Patient);
    }

    public void notACommunityMember(OpenMRSPatient patient) {
        avniBahmniErrorService.errorOccurred(patient, BahmniErrorType.NotACommunityMember);
    }

    public void processMultipleSubjectsFound(Subject subject) {
        avniBahmniErrorService.errorOccurred(subject, BahmniErrorType.MultipleSubjectsWithId);
    }

    public void processSubjectIdNull(Subject subject) {
        avniBahmniErrorService.errorOccurred(subject, BahmniErrorType.SubjectIdNull);
    }
}
