package org.avni_integration_service.bahmni.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.bahmni.BahmniMappingGroup;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.ConstantKey;
import org.avni_integration_service.bahmni.contract.*;
import org.avni_integration_service.avni.domain.Enrolment;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.repository.intmapping.MappingService;
import org.avni_integration_service.integration_data.domain.*;
import org.avni_integration_service.integration_data.repository.ConstantsRepository;
import org.avni_integration_service.bahmni.repository.OpenMRSVisitRepository;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class VisitService {
    private final ConstantsRepository constantsRepository;
    private final OpenMRSVisitRepository openMRSVisitRepository;
    private final MappingService mappingService;
    private final BahmniMappingGroup bahmniMappingGroup;
    private final BahmniMappingType bahmniMappingType;
    private static final Logger logger = Logger.getLogger(VisitService.class);

    public VisitService(ConstantsRepository constantsRepository, OpenMRSVisitRepository openMRSVisitRepository,
                        MappingService mappingService, BahmniMappingGroup bahmniMappingGroup, BahmniMappingType bahmniMappingType) {
        this.constantsRepository = constantsRepository;
        this.openMRSVisitRepository = openMRSVisitRepository;
        this.mappingService = mappingService;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
    }

    public OpenMRSVisit getVisitForPatientByDate(String patientUuid, Date date) {
        Constants allConstants = constantsRepository.findAllConstants();
        String locationUuid = allConstants.getValue(ConstantKey.IntegrationBahmniLocation.name());
        String visitTypeUuid = allConstants.getValue(ConstantKey.IntegrationBahmniVisitType.name());
        return openMRSVisitRepository.getVisit(patientUuid, locationUuid, visitTypeUuid, date);
    }

    private OpenMRSVisit getAvniRegistrationVisit(String patientUuid, Enrolment enrolment, String visitTypeUuid) {
        var avniUuidVisitAttributeTypeUuid = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniUUIDVisitAttributeType);
        String locationUuid = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniLocation.name());
        var visits = openMRSVisitRepository.getVisits(patientUuid, locationUuid, visitTypeUuid);
        return visits.stream()
                .filter(visit -> matchesEnrolmentId(visit, enrolment, avniUuidVisitAttributeTypeUuid))
                .findFirst().orElse(null);
    }

    private OpenMRSVisit createVisit(OpenMRSPatient patient, Subject subject, Date date) {
        String location = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniLocation.name());
        String visitType = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniVisitType.name());
        return createVisit(patient, location, visitType, visitAttributes(subject), date);
    }

    private OpenMRSVisit createVisit(OpenMRSPatient patient, Enrolment enrolment, Date date) {
        String location = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniLocation.name());
        String visitType = mappingService.getBahmniValue(bahmniMappingGroup.programEnrolment, bahmniMappingType.communityEnrolmentVisitType, enrolment.getProgram());
        return createVisit(patient, location, visitType, visitAttributes(enrolment), date);
    }

    private OpenMRSVisit createVisit(OpenMRSPatient patient, String location, String visitType, List<OpenMRSSaveVisitAttribute> visitAttributes, Date date) {
        OpenMRSSaveVisit openMRSSaveVisit = new OpenMRSSaveVisit();
        openMRSSaveVisit.setLocation(location);
        openMRSSaveVisit.setVisitType(visitType);
        openMRSSaveVisit.setPatient(patient.getUuid());
        openMRSSaveVisit.setStartDatetime(getVisitStartDateTimeString(date));
        openMRSSaveVisit.setStopDatetime(getVisitStopDateTimeString(date));
        openMRSSaveVisit.setAttributes(visitAttributes);
        OpenMRSVisit visit = openMRSVisitRepository.createVisit(openMRSSaveVisit);
        logger.debug("Created new visit with uuid %s".formatted(visit.getUuid()));
        return visit;
    }

    private OpenMRSVisit createVisit(OpenMRSPatient patient, String location, String visitType, Date date) {
        OpenMRSSaveVisit openMRSSaveVisit = new OpenMRSSaveVisit();
        openMRSSaveVisit.setLocation(location);
        openMRSSaveVisit.setVisitType(visitType);
        openMRSSaveVisit.setPatient(patient.getUuid());
        openMRSSaveVisit.setStartDatetime(getVisitStartDateTimeString(date));
        openMRSSaveVisit.setStopDatetime(getVisitStopDateTimeString(date));
        OpenMRSVisit visit = openMRSVisitRepository.createVisit(openMRSSaveVisit);
        logger.debug("Created new visit with uuid %s".formatted(visit.getUuid()));
        return visit;
    }

    private List<OpenMRSSaveVisitAttribute> visitAttributes(Subject subject) {
        String avniIdAttributeType = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniUUIDVisitAttributeType);
        var avniIdAttribute = new OpenMRSSaveVisitAttribute();
        avniIdAttribute.setAttributeType(avniIdAttributeType);
        avniIdAttribute.setValue(subject.getUuid());

        String avniEventDateAttributeType = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniEventDateVisitAttributeType);
        var eventDateAttribute = new OpenMRSSaveVisitAttribute();
        eventDateAttribute.setAttributeType(avniEventDateAttributeType);
        eventDateAttribute.setValue(FormatAndParseUtil.toISODateString(subject.getRegistrationDate()));

        return List.of(avniIdAttribute, eventDateAttribute);
    }

    private List<OpenMRSSaveVisitAttribute> visitAttributes(Enrolment enrolment) {
        String avniIdAttributeType = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniUUIDVisitAttributeType);
        var avniIdAttribute = new OpenMRSSaveVisitAttribute();
        avniIdAttribute.setAttributeType(avniIdAttributeType);
        avniIdAttribute.setValue(enrolment.getUuid());

        String avniEventDateAttributeType = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniEventDateVisitAttributeType);
        var eventDateAttribute = new OpenMRSSaveVisitAttribute();
        eventDateAttribute.setAttributeType(avniEventDateAttributeType);
        eventDateAttribute.setValue(FormatAndParseUtil.toISODateString(enrolment.getEnrolmentDateTime()));

        return List.of(avniIdAttribute, eventDateAttribute);
    }

    public OpenMRSVisit getOrCreateVisit(OpenMRSPatient patient, Subject subject, Date date) {
        var visit = getVisitForPatientByDate(patient.getUuid(), date);
        if (visit == null) {
            return createVisit(patient, subject, date);
        }
        logger.debug("Retrieved existing visit with uuid %s".formatted(visit.getUuid()));
        return visit;
    }

    public OpenMRSVisit getOrCreateVisit(OpenMRSPatient patient, Date date) {
        var visit = getVisitForPatientByDate(patient.getUuid(),date);
        if (visit == null) {
            String visitTypeUuid = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniVisitType.name());
            String locationUuid = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniLocation.name());
            return createVisit(patient, locationUuid, visitTypeUuid, date);
        }
        logger.debug("Retrieved existing visit with uuid %s".formatted(visit.getUuid()));
        return visit;
    }

    public OpenMRSVisit getOrCreateVisit(OpenMRSPatient patient, Enrolment enrolment) {
        var visitTypeUuid = mappingService.getBahmniValue(bahmniMappingGroup.programEnrolment,
                bahmniMappingType.communityEnrolmentVisitType,
                enrolment.getProgram());
        var visit = getAvniRegistrationVisit(patient.getUuid(), enrolment, visitTypeUuid);
        if (visit == null) {
            return createVisit(patient, enrolment, enrolment.getEnrolmentDateTime());
        }
        logger.debug("Retrieved existing visit with uuid %s".formatted(visit.getUuid()));
        return visit;
    }

    private boolean matchesEnrolmentId(OpenMRSVisit visit, Enrolment enrolment, String avniUuidVisitAttributeTypeUuid) {
        return visit.getAttributes().stream().anyMatch(visitAttribute ->
                visitAttribute.getAttributeType().getUuid().equals(avniUuidVisitAttributeTypeUuid)
                && visitAttribute.getValue().equals(enrolment.getUuid()));
    }

    public void voidVisit(Enrolment enrolment, OpenMRSFullEncounter communityEnrolmentEncounter) {
        Constants allConstants = constantsRepository.findAllConstants();
        String locationUuid = allConstants.getValue(ConstantKey.IntegrationBahmniLocation.name());
        String visitType = mappingService.getBahmniValue(bahmniMappingGroup.programEnrolment, bahmniMappingType.communityEnrolmentVisitType, enrolment.getProgram());
        OpenMRSVisit visit = openMRSVisitRepository.getVisit(communityEnrolmentEncounter.getPatient().getUuid(), locationUuid, visitType, FormatAndParseUtil.fromIsoDateString(communityEnrolmentEncounter.getEncounterDatetime()));
        openMRSVisitRepository.deleteVisit(visit.getUuid());
    }

    private String getVisitStartDateTimeString(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return FormatAndParseUtil.toISODateStringWithTimezone(calendar.getTime());
    }

    private String getVisitStopDateTimeString(Date date){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY,23);
        calendar.set(Calendar.MINUTE,59);
        calendar.set(Calendar.SECOND,59);
        return FormatAndParseUtil.toISODateStringWithTimezone(calendar.getTime());
    }
}
