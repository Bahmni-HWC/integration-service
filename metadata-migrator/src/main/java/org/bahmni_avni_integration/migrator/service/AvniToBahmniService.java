package org.bahmni_avni_integration.migrator.service;

import org.apache.log4j.Logger;
import org.bahmni_avni_integration.integration_data.domain.*;
import org.bahmni_avni_integration.integration_data.domain.MappingGroup;
import org.bahmni_avni_integration.integration_data.domain.MappingType;
import org.bahmni_avni_integration.integration_data.domain.ObsDataType;
import org.bahmni_avni_integration.integration_data.repository.MappingMetaDataRepository;
import org.bahmni_avni_integration.integration_data.ConnectionFactory;
import org.bahmni_avni_integration.migrator.domain.*;
import org.bahmni_avni_integration.migrator.repository.AvniRepository;
import org.bahmni_avni_integration.migrator.repository.ImplementationConfigurationRepository;
import org.bahmni_avni_integration.migrator.repository.OpenMRSRepository;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static java.util.Map.entry;
import static org.bahmni_avni_integration.migrator.domain.AvniFormType.*;

@Service
public class AvniToBahmniService {
    private final OpenMRSRepository openMRSRepository;
    private final AvniRepository avniRepository;
    private final MappingMetaDataRepository mappingMetaDataRepository;
    private final ConnectionFactory connectionFactory;
    private final ImplementationConfigurationRepository implementationConfigurationRepository;
    private static Logger logger = Logger.getLogger(AvniToBahmniService.class);

    public AvniToBahmniService(OpenMRSRepository openMRSRepository,
                               AvniRepository avniRepository,
                               MappingMetaDataRepository mappingMetaDataRepository,
                               ConnectionFactory connectionFactory, ImplementationConfigurationRepository implementationConfigurationRepository) {
        this.openMRSRepository = openMRSRepository;
        this.avniRepository = avniRepository;
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.connectionFactory = connectionFactory;
        this.implementationConfigurationRepository = implementationConfigurationRepository;
    }

    private void migrateForms(Connection connection) throws SQLException {
        var forms = avniRepository.getForms();
        for (var form : forms) {
            String bahmniFormConceptUuid = UUID.randomUUID().toString();
            var conceptResult = openMRSRepository.createSetConcept(connection, bahmniFormConceptUuid, getConceptSetName(form));
            if (!conceptResult.conceptExists()) {
                int formConceptId = conceptResult.conceptId();
                logger.debug("Form: %s Concept Id: %d".formatted(getConceptSetName(form), formConceptId));
                saveFormMapping(form, bahmniFormConceptUuid);
                var formElementGroups = form.getFormElementGroups();
                for (AvniFormElementGroup formElementGroup : formElementGroups) {
                    createQuestions(connection, formElementGroup.getAvniFormElements(), formConceptId);
                }
            }
            createEncounterTypeAndMapping(connection, form);
        }
    }

    private void migrateConcepts(Connection connection) throws SQLException {
        var concepts = avniRepository.getConcepts();
        for (int i = 0; i < concepts.size(); i++) {
            logger.debug("%d concepts created".formatted(i + 1));
            AvniConcept concept = concepts.get(i);
            var bahmniConceptUuid = UUID.randomUUID().toString();
            openMRSRepository.createConcept(connection,
                    bahmniConceptUuid, concept.getName(), concept.getDataType().getBahmniDataType(), "Misc", false);
            saveObsMapping(concept.getName(), bahmniConceptUuid, ObsDataType.parseAvniDataType(concept.getDataType().name()));
        }

        for (var concept : concepts) {
            if (concept.isCoded()) {
                createConceptAnswers(connection, concept);
            }
        }
    }

    private void createEncounterTypeAndMapping(Connection connection, AvniForm form) throws SQLException {
        if (form.getFormType().equals(ProgramEncounter)) {
            var encounterTypeUuid = UUID.randomUUID().toString();
            openMRSRepository.createEncounterType(connection,
                    NameMapping.fromAvniNameToBahmni(form.getEncounterType()),
                    encounterTypeUuid);
            mappingMetaDataRepository.save(mappingMetadata(MappingGroup.ProgramEncounter,
                    MappingType.CommunityProgramEncounter_EncounterType,
                    encounterTypeUuid,
                    form.getEncounterType(),
                    "Encounter type in OpenMRS for encounter type in Avni",
                    null));

        } else if (form.getFormType().equals(ProgramEnrolment)) {
            var openMrsEncounterTypeUuid = UUID.randomUUID().toString();
            openMRSRepository.createEncounterType(connection,
                    NameMapping.fromAvniNameToBahmni(String.format("%s Enrolment", form.getProgram())),
                    openMrsEncounterTypeUuid);
            mappingMetaDataRepository.save(mappingMetadata(MappingGroup.ProgramEnrolment,
                    MappingType.CommunityEnrolment_EncounterType,
                    openMrsEncounterTypeUuid,
                    form.getProgram(),
                    "Encounter type in OpenMRS for program enrolment data in Avni",
                    null));

        } else if (form.getFormType().equals(ProgramExit)) {
            var openMrsEncounterTypeUuid = UUID.randomUUID().toString();
            openMRSRepository.createEncounterType(connection,
                    NameMapping.fromAvniNameToBahmni(String.format("%s Exit", form.getProgram())),
                    openMrsEncounterTypeUuid);
            mappingMetaDataRepository.save(mappingMetadata(MappingGroup.ProgramEnrolment,
                    MappingType.CommunityEnrolmentExit_EncounterType,
                    openMrsEncounterTypeUuid,
                    form.getProgram(),
                    "Encounter type in OpenMRS for program exit data in Avni",
                    null));

        } else if (form.getFormType().equals(Encounter)) {
            var encounterTypeUuid = UUID.randomUUID().toString();
            openMRSRepository.createEncounterType(connection,
                    NameMapping.fromAvniNameToBahmni(form.getEncounterType()),
                    encounterTypeUuid);
            mappingMetaDataRepository.save(mappingMetadata(MappingGroup.GeneralEncounter,
                    MappingType.CommunityEncounter_EncounterType,
                    encounterTypeUuid,
                    form.getEncounterType(),
                    "Encounter type in OpenMRS for encounter type in Avni",
                    null));

        }
    }

    private void createQuestions(Connection connection, List<AvniFormElement> formElements, int formConceptId) throws SQLException {
        for (int i = 0; i < formElements.size(); i++) {
            var formElement = formElements.get(i);
            var concept = formElement.getConcept();
            int questionConceptId = openMRSRepository.getConceptIdByFullySpecifiedName(connection, NameMapping.fromAvniNameToBahmni(concept.getName()));
            openMRSRepository.addToConceptSet(connection, questionConceptId, formConceptId, i);
        }
    }

    private void saveObsMapping(String avniValue, String bahmniValue, ObsDataType obsDataType) {
        var existingMapping = mappingMetaDataRepository.findByMappingGroupAndMappingTypeAndAvniValue(MappingGroup.Observation,
                MappingType.Concept, avniValue);
        if (existingMapping == null) {
            mappingMetaDataRepository.saveMapping(MappingGroup.Observation,
                    MappingType.Concept,
                    bahmniValue,
                    avniValue,
                    obsDataType
            );
        }
    }

    private void saveFormMapping(AvniForm avniForm, String bahmniValue) {
        mappingMetaDataRepository.saveMapping(getMappingGroup(avniForm), getMappingType(avniForm), bahmniValue, getAvniValueForMapping(avniForm));
    }

    private MappingGroup getMappingGroup(AvniForm avniForm) {
        var formType = avniForm.getFormType();
        return switch (formType) {
            case IndividualProfile -> MappingGroup.PatientSubject;
            case Encounter -> MappingGroup.GeneralEncounter;
            case ProgramEncounter -> MappingGroup.ProgramEncounter;
            case ProgramEnrolment, ProgramExit -> MappingGroup.ProgramEnrolment;
        };
    }

    private MappingType getMappingType(AvniForm avniForm) {
        var formType = avniForm.getFormType();
        return switch (formType) {
            case IndividualProfile -> MappingType.CommunityRegistration_BahmniForm;
            case Encounter -> MappingType.CommunityEncounter_BahmniForm;
            case ProgramEncounter -> MappingType.CommunityProgramEncounter_BahmniForm;
            case ProgramEnrolment -> MappingType.CommunityEnrolment_BahmniForm;
            case ProgramExit -> MappingType.CommunityEnrolmentExit_BahmniForm;
        };
    }

    private String getAvniValueForMapping(AvniForm avniForm) {
        var formType = avniForm.getFormType();
        return switch (formType) {
            case IndividualProfile -> null;
            case Encounter, ProgramEncounter -> avniForm.getEncounterType();
            case ProgramEnrolment, ProgramExit -> avniForm.getProgram();
        };
    }

    private String getConceptSetName(AvniForm avniForm) {
        var formType = avniForm.getFormType();
        return switch (formType) {
            case IndividualProfile -> String.format("%s Registration", avniForm.getSubjectType());
            case Encounter, ProgramEncounter -> String.format("%s Encounter", avniForm.getEncounterType());
            case ProgramEnrolment -> String.format("%s Enrolment", avniForm.getProgram());
            case ProgramExit -> String.format("%s Exit", avniForm.getProgram());
        };
    }

    private void createConceptAnswers(Connection connection, AvniConcept questionConcept) throws SQLException {
        var answerConcepts = questionConcept.getAnswerConcepts();
        for (int i = 0; i < answerConcepts.size(); i++) {
            var answerConcept = answerConcepts.get(i);
            int questionConceptId = openMRSRepository.getConceptIdByFullySpecifiedName(connection, NameMapping.fromAvniNameToBahmni(questionConcept.getName()));
            int answerConceptId = openMRSRepository.getConceptIdByFullySpecifiedName(connection, NameMapping.fromAvniNameToBahmni(answerConcept.getName()));
            openMRSRepository.createConceptAnswer(connection, questionConceptId, answerConceptId, i);
        }
    }

    public void cleanup() throws SQLException {
        openMRSRepository.cleanup();
    }

    public void migrate() {
        try (var connection = connectionFactory.getOpenMRSDbConnection()) {
            connection.setAutoCommit(false);
            try {
                migrateStandardMetadata(connection);
                migrateConcepts(connection);
                migrateForms(connection);
                connection.commit();
            } catch (SQLException sqlException) {
                connection.rollback();
                throw sqlException;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException sqlException) {
            logger.error("Avni to Bahmni migration failed", sqlException);
        }

    }

    private void migrateStandardMetadata(Connection connection) throws SQLException {
        Map<String, Object> constants = implementationConfigurationRepository.getConstants();
        openMRSRepository.createAddConceptProcedure(connection);
        createEntityConceptAndMapping(connection);
        createStandardConceptsAndMappings(connection);
        createCommunityLocationAndMapping(connection, constants);
        createCommunityVisitTypeAndMapping(connection, constants);
        createRegistrationEncounterTypeAndMapping(connection);
    }

    private void createRegistrationEncounterTypeAndMapping(Connection connection) throws SQLException {
        var registrationEncounterTypeUuid = UUID.randomUUID().toString();
        openMRSRepository.createEncounterType(connection, "Community Registration", registrationEncounterTypeUuid);
        mappingMetaDataRepository.save(mappingMetadata(MappingGroup.PatientSubject,
                MappingType.Subject_EncounterType,
                registrationEncounterTypeUuid,
                null,
                "Encounter type in OpenMRS for subject registration data in Avni",
                null));
    }

    private void createCommunityVisitTypeAndMapping(Connection connection, Map<String, Object> constants) throws SQLException {
        var integrationBahmniVisitTypeUuid = (String) constants.get(ConstantKey.IntegrationBahmniVisitType.name());
        openMRSRepository.createVisitType(connection, "Community", integrationBahmniVisitTypeUuid);
    }

    private void createCommunityLocationAndMapping(Connection connection, Map<String, Object> constants) throws SQLException {
        var integrationBahmniLocationUuid = (String) constants.get(ConstantKey.IntegrationBahmniLocation.name());
        openMRSRepository.createLocation(connection, "Community", integrationBahmniLocationUuid);
    }

    private void createStandardConceptsAndMappings(Connection connection) throws SQLException {
        var standardConceptMappings = standardConceptMappings();
        for (Map<String, String> mapping : standardConceptMappings) {
            var conceptUuid = UUID.randomUUID().toString();
            String conceptName = mapping.get("conceptName");
            String conceptDataType = mapping.get("dataType");
            createStandardConceptAndMapping(connection, conceptUuid, conceptName, conceptDataType, conceptName, null);
        }
    }

    private void createEntityConceptAndMapping(Connection connection) throws SQLException {
        var entityConceptUuid = UUID.randomUUID().toString();
        openMRSRepository.createConcept(connection,
                entityConceptUuid,
                "Avni Entity UUID",
                "Text",
                "Misc",
                false);
        mappingMetaDataRepository.save(mappingMetadata(MappingGroup.Common,
                MappingType.AvniUUID_Concept,
                entityConceptUuid,
                null,
                "External uuid is used to match entities after first save",
                null));
    }

    private void createStandardConceptAndMapping(Connection connection, String conceptUuid, String conceptName, String conceptDataType, String avniValue, String about) throws SQLException {
        openMRSRepository.createConcept(connection, conceptUuid, conceptName, conceptDataType, "Misc", false);
        mappingMetaDataRepository.save(mappingMetadata(MappingGroup.Observation,
                MappingType.Concept,
                conceptUuid,
                avniValue,
                about,
                ObsDataType.parseAvniDataType(conceptDataType)));
    }

    private List<Map<String, String>> standardConceptMappings() {
        List<Map<String, String>> mappings = new ArrayList<>();
        mappings.add(Map.ofEntries(
                entry("conceptName", "Registration date"),
                entry("dataType", "Text")));

        mappings.add(Map.ofEntries(
                entry("conceptName", "First name"),
                entry("dataType", "Text")));

        mappings.add(Map.ofEntries(
                entry("conceptName", "Last name"),
                entry("dataType", "Text")));

        mappings.add(Map.ofEntries(
                entry("conceptName", "Date of birth"),
                entry("dataType", "Date")));

        mappings.add(Map.ofEntries(
                entry("conceptName", "Gender"),
                entry("dataType", "Text")));
        return mappings;
    }

    private MappingMetaData mappingMetadata(MappingGroup mappingGroup, MappingType mappingType, String bahmniValue, String avniValue, String about, ObsDataType obsDataType) {
        MappingMetaData mappingMetaData = new MappingMetaData();
        mappingMetaData.setMappingGroup(mappingGroup);
        mappingMetaData.setMappingType(mappingType);
        mappingMetaData.setBahmniValue(bahmniValue);
        mappingMetaData.setAvniValue(avniValue);
        mappingMetaData.setAbout(about);
        mappingMetaData.setDataTypeHint(obsDataType);
        return mappingMetaData;
    }
}