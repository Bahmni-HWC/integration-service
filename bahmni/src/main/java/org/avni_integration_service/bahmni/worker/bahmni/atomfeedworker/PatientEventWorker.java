package org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.MultipleResultsFoundException;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.worker.ErrorRecordWorker;
import org.avni_integration_service.bahmni.BahmniEntityType;
import org.avni_integration_service.bahmni.PatientToSubjectMetaData;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.service.AvniBahmniErrorService;
import org.avni_integration_service.bahmni.service.MappingMetaDataService;
import org.avni_integration_service.bahmni.service.PatientService;
import org.avni_integration_service.bahmni.service.SubjectService;
import org.avni_integration_service.integration_data.domain.Constants;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Component
public class PatientEventWorker implements EventWorker, ErrorRecordWorker {
    private static final Logger logger = Logger.getLogger(PatientEventWorker.class);

    @Autowired
    private PatientService patientService;

    @Autowired
    private MappingMetaDataService mappingMetaDataService;

    @Autowired
    private AvniBahmniErrorService avniBahmniErrorService;

    @Autowired
    private SubjectService subjectService;

    private Constants constants;
    private PatientToSubjectMetaData metaData;

    @Value("${bahmni.app.first.run}")
    private boolean isFirstRun;

    @Value("${bahmni.avni.address.knownSubDistricts}")
    private String knownSubDistricts;

    @Value("${bahmni.avni.address.knownVillages}")
    private String knownVillages;


    @Override
    public void process(Event event) {
        OpenMRSPatient patient = patientService.getPatient(event);
        if (patient == null) {
            logger.warn(String.format("Feed out of sync with the actual data: %s", event.toString()));
            return;
        }
        if (patientService.shouldFilterPatient(patient, constants)) {
            logger.warn(String.format("Patient should be filtered out: %s", patient.getPatientId()));
            return;
        }
        processPatient(patient);
    }

    private void processPatient(OpenMRSPatient patient) {
        logger.debug(String.format("Processing patient: name %s || uuid %s", patient.getName(), patient.getUuid()));

        GeneralEncounter patientEncounter = subjectService.findPatient(metaData, patient.getUuid());
        if (isFirstRun) {
            if (patientEncounter != null || avniBahmniErrorService.hasError(patient.getUuid(), BahmniEntityType.Patient)) {
                logger.info("Early return for first run, as the record is already processed before");
                return;
            }
        }

        Subject subject;
        try {
            subject = subjectService.findSubject(patient, metaData, constants);
        } catch (MultipleResultsFoundException e) {
            subjectService.processMultipleSubjectsFound(patient);
            return;
        }

        if (patientEncounter != null && subject != null) {
            subjectService.updateRegistrationEncounter(patientEncounter, patient, metaData);
        } else if (patientEncounter != null && subject == null) {
            subjectService.processSubjectIdChanged(patient);
        } else if (patientEncounter == null && subject != null) {
            subjectService.createRegistrationEncounter(patient, subject, metaData);
        } else if (patientEncounter == null && subject == null) {
            if (!isKnownPatientLocation(patient)) {
                return;
            }
            Subject avniSubject = subjectService.createSubjectFromPatient(patient, constants, metaData);
            logger.debug("Created Avni Subject: " + avniSubject);
            //subjectService.processSubjectNotFound(patient);
        }
    }

    private boolean isKnownPatientLocation(OpenMRSPatient patient) {
        if (patient.getPerson().getPreferredAddress() == null) {
            logger.error("Can not sync patients to Avni. Patient Address is null");
            return false;
        }
        String subDistrict = patient.getPerson().getPreferredAddress().getAddress4();
        String village = patient.getPerson().getPreferredAddress().getCityVillage();

        List<String> knownSubDistrictList = Arrays.asList(knownSubDistricts.split(","));
        List<String> knownVillageList = Arrays.asList(knownVillages.split(","));
        if (!knownSubDistrictList.contains(subDistrict.toUpperCase().trim())) {
            return false;
        }

        if (!knownVillageList.contains(village.toUpperCase().trim())) {
            return false;
        }

        return true;
    }

    @Override
    public void cleanUp(Event event) {
    }

    public void processError(String patientUuid) {
        OpenMRSPatient patient = patientService.getPatient(patientUuid);
        if (patient == null) {
            logger.warn(String.format("Patient has been deleted now: %s", patientUuid));
            patientService.patientDeleted(patientUuid);
            return;
        }

        if (patientService.shouldFilterPatient(patient, constants)) {
            logger.warn(String.format("Patient is not eligible for integration anymore: %s", patient.getPatientId()));
            patientService.notACommunityMember(patient);
            return;
        }

        processPatient(patient);
    }

    //    avoid loading of constants for every event
    public void cacheRunImmutables(Constants constants) {
        this.constants = constants;
        metaData = mappingMetaDataService.getForPatientToSubject();
    }
}
