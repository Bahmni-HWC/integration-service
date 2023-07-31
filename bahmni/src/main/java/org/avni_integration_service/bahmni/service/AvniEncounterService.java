package org.avni_integration_service.bahmni.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.bahmni.BahmniEncounterToAvniEncounterMetaData;
import org.avni_integration_service.bahmni.BahmniErrorType;
import org.avni_integration_service.bahmni.SubjectToPatientMetaData;
import org.avni_integration_service.bahmni.contract.OpenMRSDefaultEncounter;
import org.avni_integration_service.bahmni.contract.OpenMRSFullEncounter;
import org.avni_integration_service.bahmni.contract.OpenMRSInventoryDispenseRequest;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.mapper.DispenseRequestMapper;
import org.avni_integration_service.bahmni.mapper.OpenMRSEncounterMapper;
import org.avni_integration_service.bahmni.mapper.avni.EncounterMapper;
import org.avni_integration_service.bahmni.repository.BahmniSplitEncounter;
import org.avni_integration_service.bahmni.repository.OpenMRSEncounterRepository;
import org.avni_integration_service.bahmni.repository.intmapping.MappingService;
import org.avni_integration_service.integration_data.domain.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.avni_integration_service.bahmni.constants.DispenseConstants.AVNI_DISPENSE_ENCOUNTER_TYPE;
import static org.avni_integration_service.bahmni.constants.DispenseConstants.AVNI_DISPENSE_QUESTION_GROUP_NAME;

@Service
public class AvniEncounterService extends BaseAvniEncounterService {
    private final OpenMRSEncounterMapper openMRSEncounterMapper;

    private final AvniEncounterRepository avniEncounterRepository;

    private final OpenMRSEncounterRepository openMRSEncounterRepository;

    private final EncounterMapper encounterMapper;

    private final VisitService visitService;

    private final AvniBahmniErrorService avniBahmniErrorService;

    private final DispenseRequestMapper dispenseRequestMapper;

    private final OpenMRSInventoryService openMRSInventoryService;

    private static final Logger logger = Logger.getLogger(AvniEncounterService.class);

    @Autowired
    public AvniEncounterService(PatientService patientService, OpenMRSEncounterRepository openMRSEncounterRepository, OpenMRSEncounterMapper openMRSEncounterMapper, AvniEncounterRepository avniEncounterRepository, PatientService patientService1, MappingService mappingService, OpenMRSEncounterRepository openMRSEncounterRepository1, EncounterMapper encounterMapper, VisitService visitService, AvniBahmniErrorService avniBahmniErrorService, DispenseRequestMapper dispenseRequestMapper, OpenMRSInventoryService openMRSInventoryService) {
        super(patientService, mappingService, openMRSEncounterRepository);
        this.openMRSEncounterMapper = openMRSEncounterMapper;
        this.avniEncounterRepository = avniEncounterRepository;
        this.openMRSInventoryService = openMRSInventoryService;
        this.mappingService = mappingService;
        this.openMRSEncounterRepository = openMRSEncounterRepository1;
        this.encounterMapper = encounterMapper;
        this.visitService = visitService;
        this.avniBahmniErrorService = avniBahmniErrorService;
        this.dispenseRequestMapper = dispenseRequestMapper;
    }

    public void update(BahmniSplitEncounter bahmniSplitEncounter, GeneralEncounter existingAvniEncounter, BahmniEncounterToAvniEncounterMetaData bahmniEncounterToAvniEncounterMetaData, GeneralEncounter avniPatient) {
        GeneralEncounter encounter = openMRSEncounterMapper.mapToAvniEncounter(bahmniSplitEncounter, bahmniEncounterToAvniEncounterMetaData, avniPatient);
        avniEncounterRepository.update(existingAvniEncounter.getUuid(), encounter);
    }

    public void updateLabEncounter(OpenMRSFullEncounter fullEncounter, GeneralEncounter existingAvniEncounter, BahmniEncounterToAvniEncounterMetaData bahmniEncounterToAvniEncounterMetaData, GeneralEncounter avniPatient) {
        GeneralEncounter encounter = openMRSEncounterMapper.mapToAvniEncounter(fullEncounter, bahmniEncounterToAvniEncounterMetaData, avniPatient);
        avniEncounterRepository.update(existingAvniEncounter.getUuid(), encounter);
    }

    public GeneralEncounter getGeneralEncounter(BahmniSplitEncounter bahmniSplitEncounter, BahmniEncounterToAvniEncounterMetaData metaData) {
        Map<String, Object> obsCriteria = Map.of(metaData.getBahmniEntityUuidConcept(), bahmniSplitEncounter.getOpenMRSEncounterUuid());
        // OpenMRS encounter uuid will be shared by multiple entities in Avni, hence encounter type is required
        return avniEncounterRepository.getEncounter(metaData.getAvniMappedName(bahmniSplitEncounter.getFormConceptSetUuid()), obsCriteria);
    }

    public GeneralEncounter getGeneralEncounterByEncounterType(String avniEncounterType, String openMRSEncounterUUID, BahmniEncounterToAvniEncounterMetaData metaData) {
        Map<String, Object> obsCriteria = Map.of(metaData.getBahmniEntityUuidConcept(), openMRSEncounterUUID);
        return avniEncounterRepository.getEncounter(avniEncounterType, obsCriteria);
    }

    public GeneralEncounter getLabResultGeneralEncounter(OpenMRSFullEncounter openMRSFullEncounter, BahmniEncounterToAvniEncounterMetaData metaData) {
        Map<String, Object> obsCriteria = Map.of(metaData.getBahmniEntityUuidConcept(), openMRSFullEncounter.getUuid());
        return avniEncounterRepository.getEncounter(metaData.getLabEncounterTypeMapping().getAvniValue(), obsCriteria);
    }

    public void create(BahmniSplitEncounter splitEncounter, BahmniEncounterToAvniEncounterMetaData metaData, GeneralEncounter avniPatient) {
        if (splitEncounter.isVoided()) return;

        GeneralEncounter encounter = openMRSEncounterMapper.mapToAvniEncounter(splitEncounter, metaData, avniPatient);
        avniEncounterRepository.create(encounter);
    }

    public void createLabEncounter(OpenMRSFullEncounter openMRSFullEncounter, BahmniEncounterToAvniEncounterMetaData metaData, GeneralEncounter avniPatient) {
        if (openMRSFullEncounter.isVoided()) return;

        GeneralEncounter encounter = openMRSEncounterMapper.mapToAvniEncounter(openMRSFullEncounter, metaData, avniPatient);
        avniEncounterRepository.create(encounter);
    }

    public void createDrugOrderEncounter(OpenMRSFullEncounter openMRSFullEncounter, BahmniEncounterToAvniEncounterMetaData metaData, GeneralEncounter avniPatient, OpenMRSDefaultEncounter defaultEncounter) {
        if (openMRSFullEncounter.isVoided()) return;

        GeneralEncounter encounter = openMRSEncounterMapper.mapDrugOrderEncounterToAvniEncounter(openMRSFullEncounter, metaData, avniPatient, defaultEncounter);
        avniEncounterRepository.create(encounter);
    }

    public void updateDrugOrderEncounter(OpenMRSFullEncounter fullEncounter, GeneralEncounter existingAvniEncounter, BahmniEncounterToAvniEncounterMetaData bahmniEncounterToAvniEncounterMetaData, GeneralEncounter avniPatient, OpenMRSDefaultEncounter defaultEncounter) {
        GeneralEncounter encounter = openMRSEncounterMapper.mapDrugOrderEncounterToAvniEncounter(fullEncounter, bahmniEncounterToAvniEncounterMetaData, avniPatient, defaultEncounter);
        avniEncounterRepository.update(existingAvniEncounter.getUuid(), encounter);
    }

    public GeneralEncounter getDrugOrderGeneralEncounter(OpenMRSFullEncounter openMRSEncounter, BahmniEncounterToAvniEncounterMetaData metaData) {
        Map<String, Object> obsCriteria = Map.of(metaData.getBahmniEntityUuidConcept(), openMRSEncounter.getUuid());
        // OpenMRS encounter uuid will be shared by multiple entities in Avni, hence encounter type is required
        return avniEncounterRepository.getEncounter(metaData.getDrugOrderEncounterTypeMapping().getAvniValue(), obsCriteria);
    }

    public boolean shouldFilterEncounter(GeneralEncounter generalEncounter) {
        return !generalEncounter.isCompleted();
    }

    public OpenMRSFullEncounter createCommunityEncounter(GeneralEncounter generalEncounter, OpenMRSPatient patient, Constants constants, Subject subject) {
        if (generalEncounter.getVoided()) {
            logger.debug(String.format("Skipping voided Avni encounter %s", generalEncounter.getUuid()));
            return null;
        }
        var visit = visitService.getOrCreateVisit(patient, subject, generalEncounter.getEncounterDateTime());
        logger.debug(String.format("Creating new Bahmni Encounter for Avni general encounter %s", generalEncounter.getUuid()));
        var openMRSEncounter = encounterMapper.mapEncounter(generalEncounter, patient.getUuid(), constants, visit);
        var savedEncounter = openMRSEncounterRepository.createEncounter(openMRSEncounter);

        avniBahmniErrorService.successfullyProcessed(generalEncounter);
        return savedEncounter;
    }

    public void updateCommunityEncounter(OpenMRSFullEncounter existingEncounter, GeneralEncounter generalEncounter, Constants constants) {
        if (generalEncounter.getVoided()) {
            logger.debug(String.format("Voiding Bahmni Encounter %s because the Avni general encounter %s is voided",
                    existingEncounter.getUuid(),
                    generalEncounter.getUuid()));
            openMRSEncounterRepository.voidEncounter(existingEncounter);
        } else {
            logger.debug(String.format("Updating existing Bahmni general encounter %s", existingEncounter.getUuid()));
            var openMRSEncounter = encounterMapper.mapEncounterToExistingEncounter(existingEncounter,
                    generalEncounter,
                    constants);
            openMRSEncounterRepository.updateEncounter(openMRSEncounter);
            avniBahmniErrorService.successfullyProcessed(generalEncounter);
        }
    }

    public void processPatientNotFound(GeneralEncounter encounter) {
        avniBahmniErrorService.errorOccurred(encounter, BahmniErrorType.NoPatientWithId);
    }

    public GeneralEncounter getDiagnosesGeneralEncounter(OpenMRSFullEncounter openMRSEncounter, BahmniEncounterToAvniEncounterMetaData metaData) {
        Map<String, Object> obsCriteria = Map.of(metaData.getBahmniEntityUuidConcept(), openMRSEncounter.getUuid());
        return avniEncounterRepository.getEncounter(metaData.getDiagnosesEncounterTypeMapping().getAvniValue(), obsCriteria);
    }

    public void createDiagnosesEncounter(OpenMRSFullEncounter openMRSEncounter, BahmniEncounterToAvniEncounterMetaData metaData, GeneralEncounter avniPatient) {
        if (openMRSEncounter.isVoided()) return;

        GeneralEncounter encounter = openMRSEncounterMapper.mapDiagnosesObsToAvniEncounter(openMRSEncounter, metaData, avniPatient);
        avniEncounterRepository.create(encounter);
    }

    public void updateDiagnosesEncounter(OpenMRSFullEncounter openMRSEncounter, GeneralEncounter existingAvniEncounter, BahmniEncounterToAvniEncounterMetaData metaData, GeneralEncounter avniPatient) {
        GeneralEncounter encounter = openMRSEncounterMapper.mapDiagnosesObsToAvniEncounter(openMRSEncounter, metaData, avniPatient);
        avniEncounterRepository.update(existingAvniEncounter.getUuid(), encounter);
    }

    public void create(OpenMRSFullEncounter openMRSFullEncounter, String avniEncounterType, List<Map<String, Object>> form2Obs, BahmniEncounterToAvniEncounterMetaData metaData, GeneralEncounter avniPatient) {
        if (openMRSFullEncounter.isVoided()) {
            logger.info(String.format("Skipping voided Bahmni encounter: %s", openMRSFullEncounter.getUuid()));
            return;
        }

        GeneralEncounter encounter = openMRSEncounterMapper.mapToAvniEncounter(openMRSFullEncounter, avniEncounterType, form2Obs, metaData, avniPatient);
        avniEncounterRepository.create(encounter);
    }

    public void update(OpenMRSFullEncounter openMRSFullEncounter, GeneralEncounter existingAvniEncounter, List<Map<String, Object>> form2Obs, BahmniEncounterToAvniEncounterMetaData metaData, GeneralEncounter avniPatient) {
        GeneralEncounter encounter = openMRSEncounterMapper.mapToAvniEncounter(openMRSFullEncounter, existingAvniEncounter.getEncounterType(), form2Obs, metaData, avniPatient);
        avniEncounterRepository.update(existingAvniEncounter.getUuid(), encounter);
    }

    public boolean isDispenseEncounter(GeneralEncounter generalEncounter) {
        return generalEncounter.getEncounterType().equals(AVNI_DISPENSE_ENCOUNTER_TYPE);
    }

    public void processDispenseEncounter(GeneralEncounter generalEncounter, SubjectToPatientMetaData metaData, Constants constants, Subject subject) {
        OpenMRSPatient openMRSPatient = patientService.findPatient(subject, constants, metaData);
        if (openMRSPatient == null) {
            processPatientNotFound(generalEncounter);
            return;
        }
        List<Map<String, Object>> avniDispenseObservations = (List<Map<String, Object>>) generalEncounter.getObservations().get(AVNI_DISPENSE_QUESTION_GROUP_NAME);
        if (avniDispenseObservations == null) {
            logger.info(String.format("Skipping dispense encounter %s as it does not have any dispense observations", generalEncounter.getUuid()));
            return;
        }
        try {
            OpenMRSInventoryDispenseRequest openMRSInventoryDispenseRequest = dispenseRequestMapper.mapToOpenMRSInventoryDispenseRequest(openMRSPatient.getUuid(), subject.getCatchments().get(0), avniDispenseObservations);
            openMRSInventoryService.dispense(openMRSInventoryDispenseRequest);
            avniBahmniErrorService.successfullyProcessed(generalEncounter);
        } catch (Exception e) {
            logger.error(String.format("Error while processing dispense encounter %s", generalEncounter.getUuid()), e);
            avniBahmniErrorService.errorOccurred(generalEncounter, BahmniErrorType.DispenseError);
        }

    }
}
