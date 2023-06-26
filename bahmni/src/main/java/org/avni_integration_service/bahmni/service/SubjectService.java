package org.avni_integration_service.bahmni.service;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.bahmni.*;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.contract.OpenMRSPatientIdentifier;
import org.avni_integration_service.bahmni.mapper.OpenMRSPatientMapper;
import org.avni_integration_service.bahmni.repository.intmapping.MappingService;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

@Service
public class SubjectService {
    private final AvniEncounterRepository avniEncounterRepository;
    private final AvniSubjectRepository avniSubjectRepository;
    private final MappingService mappingService;
    private final AvniBahmniErrorService avniBahmniErrorService;
    private final OpenMRSPatientMapper openMRSPatientMapper;
    private final BahmniMappingGroup bahmniMappingGroup;
    private final BahmniMappingType bahmniMappingType;

    private final Map<String, String> genderMap = Map.of("M","Male", "F", "Female", "O", "Others");

    public SubjectService(AvniEncounterRepository avniEncounterRepository, AvniSubjectRepository avniSubjectRepository,
                          MappingService mappingService, AvniBahmniErrorService avniBahmniErrorService,
                          OpenMRSPatientMapper openMRSPatientMapper, BahmniMappingGroup bahmniMappingGroup,
                          BahmniMappingType bahmniMappingType) {
        this.avniEncounterRepository = avniEncounterRepository;
        this.avniSubjectRepository = avniSubjectRepository;
        this.mappingService = mappingService;
        this.avniBahmniErrorService = avniBahmniErrorService;
        this.openMRSPatientMapper = openMRSPatientMapper;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
    }

    public Subject findSubject(OpenMRSPatient openMRSPatient, PatientToSubjectMetaData patientToSubjectMetaData, Constants constants) {
        String identifier = openMRSPatient.getPatientId();
        LinkedHashMap<String, Object> subjectCriteria = new LinkedHashMap<>();
        String prefix = constants.getValue(ConstantKey.BahmniIdentifierPrefix.name());
        subjectCriteria.put(patientToSubjectMetaData.avniIdentifierConcept(), identifier.replace(prefix, ""));
        return avniSubjectRepository.getSubject(
                new GregorianCalendar(1900, Calendar.JANUARY, 1).getTime(),
                constants.getValue(ConstantKey.IntegrationAvniSubjectType.name()),
                subjectCriteria
        );
    }

    public Subject[] findSubjects(Subject subject, SubjectToPatientMetaData subjectToPatientMetaData, Constants constants) {
        LinkedHashMap<String, Object> subjectCriteria = new LinkedHashMap<>();
        subjectCriteria.put(subjectToPatientMetaData.avniIdentifierConcept(), subject.getId(subjectToPatientMetaData.avniIdentifierConcept()));
        return avniSubjectRepository.getSubjects(
                new GregorianCalendar(1900, Calendar.JANUARY, 1).getTime(),
                constants.getValue(ConstantKey.IntegrationAvniSubjectType.name()),
                subjectCriteria
        );
    }

    public void createRegistrationEncounter(OpenMRSPatient openMRSPatient, Subject subject, PatientToSubjectMetaData patientToSubjectMetaData) {
        if (openMRSPatient.isVoided()) return;

        MappingMetaDataCollection conceptMetaData = mappingService.findAll(bahmniMappingGroup.patientSubject, bahmniMappingType.personAttributeConcept);
        GeneralEncounter encounterRequest = openMRSPatientMapper.mapToAvniEncounter(openMRSPatient, subject, patientToSubjectMetaData, conceptMetaData);
        avniEncounterRepository.create(encounterRequest);

        avniBahmniErrorService.successfullyProcessed(openMRSPatient);
    }

    public void updateRegistrationEncounter(GeneralEncounter encounterRequest, OpenMRSPatient openMRSPatient, PatientToSubjectMetaData patientToSubjectMetaData) {
        MappingMetaDataCollection conceptMetaData = mappingService.findAll(bahmniMappingGroup.patientSubject, bahmniMappingType.personAttributeConcept);
        encounterRequest.setObservations(openMRSPatientMapper.mapToAvniObservations(openMRSPatient, patientToSubjectMetaData, conceptMetaData));
        encounterRequest.setVoided(openMRSPatient.isVoided());
        avniEncounterRepository.update((String) encounterRequest.get("ID"), encounterRequest);

        avniBahmniErrorService.successfullyProcessed(openMRSPatient);
    }

    // Patient from OpenMRS/Bahmni is saved as Encounter in Avni
    public GeneralEncounter findPatient(BahmniToAvniMetaData metaData, String externalId) {
        LinkedHashMap<String, Object> encounterCriteria = new LinkedHashMap<>();
        encounterCriteria.put(metaData.getBahmniEntityUuidConcept(), externalId);
        return avniEncounterRepository.getEncounter(encounterCriteria);
    }

    public void processSubjectIdChanged(OpenMRSPatient patient) {
        avniBahmniErrorService.errorOccurred(patient, BahmniErrorType.SubjectIdChanged);
    }

    public void processSubjectNotFound(OpenMRSPatient patient) {
        avniBahmniErrorService.errorOccurred(patient, BahmniErrorType.NoSubjectWithId);
    }

    public void processMultipleSubjectsFound(OpenMRSPatient patient) {
        avniBahmniErrorService.errorOccurred(patient, BahmniErrorType.MultipleSubjectsWithId);
    }

    public Subject createSubjectFromPatient(OpenMRSPatient patient, Constants constants, PatientToSubjectMetaData metaData) {
        Subject subject = new Subject();
        subject.setSubjectType(constants.getValue(ConstantKey.IntegrationAvniSubjectType.name()));
        //TODO verify
        subject.setRegistrationDate(FormatAndParseUtil.fromIsoDateString(patient.getAuditInfo().getDateCreated()));
        //subject.setRegistrationDate(Date.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant()));

        //Set names
        subject.setFirstName(patient.getPerson().getPreferredName().getGivenName());
        subject.setMiddleName(patient.getPerson().getPreferredName().getMiddleName());
        subject.setLastName(patient.getPerson().getPreferredName().getFamilyName());

        //set address
        Function<String, String> valueOrEmpty = s -> s != null? s : "";
        //STATE, DISTRICT, TALUK, VILLAGE
        String address = String.format("%s, %s, %s, %s",
                //valueOrEmpty.apply(patient.getPerson().getPreferredAddress().getCountry()),
                valueOrEmpty.apply(patient.getPerson().getPreferredAddress().getStateProvince()),
                valueOrEmpty.apply(patient.getPerson().getPreferredAddress().getCountyDistrict()),
                valueOrEmpty.apply(patient.getPerson().getPreferredAddress().getAddress4()),
                valueOrEmpty.apply(patient.getPerson().getPreferredAddress().getCityVillage()));
        subject.setAddress(address);
        //set dob
        subject.setDob(new SimpleDateFormat("yyyy-MM-dd").format(patient.getPerson().getBirthdate()));

        //set gender
        String genderString = genderMap.get(patient.getPerson().getGender());
        subject.setGender(genderString != null ? genderString : patient.getPerson().getGender());

        Optional<OpenMRSPatientIdentifier> identifier = patient.getIdentifiers().stream().filter(id -> id.isPreferred()).findFirst();
        subject.setExternalId(patient.getUuid());
        if (identifier.isPresent()) {
            String prefix = constants.getValue(ConstantKey.BahmniIdentifierPrefix.name());
            subject.addObservation(metaData.avniIdentifierConcept(), identifier.get().getIdentifier().replace(prefix, ""));
        }

        Optional<OpenMRSPatientIdentifier> rchIdentifier = patient.getIdentifiers().stream()
                .filter(id -> id.getIdentifierType().getUuid().equals("3766473c-0c29-11ee-be56-0242ac120002"))
                .findFirst();
        rchIdentifier.ifPresent(identifiers -> subject.addObservation("RCH ID", identifier.get().getIdentifier()));

        Optional<OpenMRSPatientIdentifier> nikshayIdentifier = patient.getIdentifiers().stream()
                .filter(id -> id.getIdentifierType().getUuid().equals("45bcdf58-0c29-11ee-be56-0242ac120002"))
                .findFirst();
        nikshayIdentifier.ifPresent(identifiers -> subject.addObservation("Nikshay ID", identifier.get().getIdentifier()));


        String phoneNumber = patient.getPerson().getAttributes().getPhoneNumber();
        if (phoneNumber != null && phoneNumber.startsWith("+91")) {
            phoneNumber = phoneNumber.substring(3);
        }
        subject.addObservation("Phone Number", phoneNumber);

        subject.addObservation("Father/Mother's Name", patient.getPerson().getAttributes().getFatherOrMotherName());

//        subject.setVoided(MapUtil.getBoolean(DemandIsVoidedField, response));
//        String[] arrayOfTCs = MapUtil.getString(DemandTargetCommunity, response) != null ? MapUtil.getString(DemandTargetCommunity, response).split(";") : null;
//        subject.addObservation("Target Community", arrayOfTCs);
        return avniSubjectRepository.create(subject);
    }
}
