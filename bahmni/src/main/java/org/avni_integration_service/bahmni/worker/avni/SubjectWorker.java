package org.avni_integration_service.bahmni.worker.avni;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.domain.SubjectsResponse;
import org.avni_integration_service.avni.repository.AvniIgnoredConceptsRepository;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.avni.worker.ErrorRecordWorker;
import org.avni_integration_service.bahmni.BahmniErrorType;
import org.avni_integration_service.bahmni.ConstantKey;
import org.avni_integration_service.bahmni.SubjectToPatientMetaData;
import org.avni_integration_service.bahmni.contract.CreateABHASuccessResponse;
import org.avni_integration_service.bahmni.contract.OpenMRSFullEncounter;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.exceptions.ABHACreationFailedException;
import org.avni_integration_service.bahmni.service.*;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEncounterEventWorker;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SubjectWorker implements ErrorRecordWorker {
    @Autowired
    private IntegratingEntityStatusRepository integrationEntityStatusRepository;
    @Autowired
    private MappingMetaDataService mappingMetaDataService;
    @Autowired
    private AvniSubjectRepository avniSubjectRepository;
    @Autowired
    private PatientService patientService;
    @Autowired
    private AvniEntityStatusService avniEntityStatusService;
    @Autowired
    private AvniBahmniErrorService avniBahmniErrorService;
    @Autowired
    private AvniIgnoredConceptsRepository avniIgnoredConceptsRepository;
    @Autowired
    private SubjectService subjectService;

    @Autowired
    private ABHAService abhaService;

    private static final Logger logger = Logger.getLogger(SubjectWorker.class);
    private SubjectToPatientMetaData metaData;
    private Constants constants;

    public void processSubjects() {
        while (true) {
            IntegratingEntityStatus status = integrationEntityStatusRepository.findByEntityType(AvniEntityType.Subject.name());
            SubjectsResponse response = avniSubjectRepository.getSubjects(status.getReadUptoDateTime(), constants.getValue(ConstantKey.IntegrationAvniSubjectType.name()));
            Subject[] subjects = response.getContent();
            int totalPages = response.getTotalPages();
            logger.info(String.format("Found %d subjects that are newer than %s", subjects.length, status.getReadUptoDateTime()));
            if (subjects.length == 0) break;
            for (Subject subject : subjects) {
                processSubject(subject, true);
            }
            if (totalPages == 1) {
                logger.info("Finished processing all pages");
                break;
            }
        }
    }

    private void removeIgnoredObservations(Subject subject) {
        var observations = (Map<String, Object>) subject.getObservations();
        avniIgnoredConceptsRepository.getIgnoredConcepts().forEach(observations::remove);
        subject.setObservations(observations);
    }

    private void updateSyncStatus(Subject subject, boolean updateSyncStatus) {
        if (updateSyncStatus)
            avniEntityStatusService.saveEntityStatus(subject);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processSubject(Subject subject, boolean updateSyncStatus) {
        logger.debug("Processing subject %s".formatted(subject.getUuid()));

        if (hasAadhaarNumber(subject)) {
            Map<String, Object> updatedSubjectObservations = new HashMap<>();

            try {
                logger.info("Initiating ABHA Creation with Aadhaar Demographics for subject %s".formatted(subject.getId(metaData.avniIdentifierConcept())));
                CreateABHASuccessResponse createABHASuccessResponse = abhaService.createABHA(subject);
                if (abhaService.isABHAAddressLinkedAlready(createABHASuccessResponse.getHealthId())) {
                    logger.info("ABHA Address %s is already linked to another Patient. Skipping ABHA mapping for %s".formatted(createABHASuccessResponse.getHealthId(), subject.getId(metaData.avniIdentifierConcept())));
                    updatedSubjectObservations.put("ABHA Creation Error", String.format("ABHA Address %s is already linked to another Patient", createABHASuccessResponse.getHealthId()));
                } else {
                    logger.info("ABHA Creation with Aadhaar Demographics successful for subject %s".formatted(subject.getId(metaData.avniIdentifierConcept())));
                    updatedSubjectObservations.put("ABHA Number", createABHASuccessResponse.getHealthIdNumber());
                    updatedSubjectObservations.put("ABHA Address", createABHASuccessResponse.getHealthId());
                    updatedSubjectObservations.put("ABHA Creation Error", null);
                }

            } catch (ABHACreationFailedException e) {
                logger.error("ABHA Creation with demographics failed for subject %s \n %s".formatted(subject.getId(metaData.avniIdentifierConcept()), e.getMessage()));
                updatedSubjectObservations.put("ABHA Creation Error", e.getMessage());
            } catch (Exception e) {
                logger.error("ABHA Creation with demographics failed for subject %s \n %s".formatted(subject.getId(metaData.avniIdentifierConcept()), e.getMessage()));
                updatedSubjectObservations.put("ABHA Creation Error", "Internal Server Error");
            } finally {
                logger.info("Updating Avni subject %s after ABHA processing".formatted(subject.getId(metaData.avniIdentifierConcept())));
                updatedSubjectObservations.put("Aadhaar Number", null);
                subject = subjectService.updateSubjectObservations(subject.getUuid(), updatedSubjectObservations, constants);

            }
        }

        if (subject.getId(metaData.avniIdentifierConcept()) == null) {
            logger.debug("Skip subject %s because of having null identifier".formatted(subject.getUuid()));
            patientService.processSubjectIdNull(subject);
            updateSyncStatus(subject, updateSyncStatus);
            return;
        }

        if (hasDuplicates(subject)) {
            if (!subject.getVoided()) {
                logger.error("Create multiple subjects found error for subject %s identifier %s".formatted(subject.getUuid(), subject.getId(metaData.avniIdentifierConcept())));
                patientService.processMultipleSubjectsFound(subject);
            } else {
                logger.debug("Skip voided subject %s because of having non voided duplicates".formatted(subject.getUuid()));
            }
            updateSyncStatus(subject, updateSyncStatus);
            return;
        }
        ;
        removeIgnoredObservations(subject);

        try {
            Pair<OpenMRSPatient, OpenMRSFullEncounter> patientEncounter = patientService.findSubject(subject, constants, metaData);
            var patient = patientEncounter.getValue0();
            var encounter = patientEncounter.getValue1();

            if (encounter != null && patient != null) {
                logger.debug(String.format("Updating existing encounter %s for subject %s", encounter.getUuid(), subject.getUuid()));
                patientService.updateSubject(encounter, patient, subject, metaData, constants);
            } else if (encounter != null && patient == null) {
                // product-roadmap-todo: openmrs doesn't support the ability to find encounter without providing the patient hence this condition will never be reached
                patientService.processPatientIdChanged(subject, metaData);
            } else if (encounter == null && patient == null) {
                logger.debug(String.format("Creating new patient and new encounter for subject %s", subject.getUuid()));
                patientService.createPatientAndSubject(subject, metaData, constants);
            }
            logger.debug(String.format("Saving entity status for subject %s", subject.getLastModifiedDate()));
        } catch (PatientEncounterEventWorker.SubjectIdChangedException e) {
            avniBahmniErrorService.errorOccurred(subject.getUuid(), BahmniErrorType.SubjectIdChanged, AvniEntityType.Subject);
        }

        updateSyncStatus(subject, updateSyncStatus);
    }

    private boolean hasDuplicates(Subject subject) {
        Subject[] subjects = subjectService.findSubjects(subject, metaData, constants);
        int sizeOfNonVoidedOtherThanSelf = Arrays.stream(subjects)
                .filter(s -> !s.getUuid().equals(subject.getUuid()))
                .filter(s -> !s.getVoided())
                .collect(Collectors.toList())
                .size();
        boolean hasDuplicates = sizeOfNonVoidedOtherThanSelf > 0;
        if (hasDuplicates) {
            logger.debug("Duplicate subjects found for subject %s identifier %s voided %s".formatted(subject.getUuid(),
                    subject.getId(metaData.avniIdentifierConcept()),
                    subject.getVoided()));
        }
        return hasDuplicates;
    }

    @Override
    public void processError(String entityUuid) {
        Subject subject = avniSubjectRepository.getSubject(entityUuid);
        if (subject == null) {
            logger.warn(String.format("Subject has been deleted now: %s", entityUuid));
            avniBahmniErrorService.errorOccurred(entityUuid, BahmniErrorType.EntityIsDeleted, AvniEntityType.Subject);
            return;
        }

        processSubject(subject, false);
    }

    public void cacheRunImmutables(Constants constants) {
        this.constants = constants;
        metaData = mappingMetaDataService.getForSubjectToPatient();
    }

    private boolean hasAadhaarNumber(Subject subject) {
        return subject.getObservations().get("Aadhaar Number") != null && !subject.getObservations().get("Aadhaar Number").toString().isBlank();
    }
}
