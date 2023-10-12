package org.avni_integration_service.bahmni.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.client.HIPWebClient;
import org.avni_integration_service.bahmni.client.OpenMRSWebClient;
import org.avni_integration_service.bahmni.contract.*;
import org.avni_integration_service.bahmni.exceptions.ABHACreationFailedException;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.text.SimpleDateFormat;
import java.util.Map;

@Service
public class ABHAService {

    private static final Logger logger = Logger.getLogger(ABHAService.class);

    private static final String DEMOGRAPHIC_AUTH_ENDPOINT = "/v1/hid/benefit/createHealthId/demo/auth";
    private static final String EXISTING_PATIENTS_ENDPOINT = "/openmrs/ws/rest/v1/hip/existingPatients/";

    private final Map<String, String> genderMap = Map.of("Male", "M", "Female", "F", "Other", "T");

    @Autowired
    private HIPWebClient hipWebClient;

    @Autowired
    private OpenMRSWebClient openMRSWebClient;

    @Value("${openmrs.uri.prefix}")
    protected String openmrsUrlPrefix;

    public CreateABHASuccessResponse createABHA(Subject subject) throws ABHACreationFailedException {
        try {
            ResponseEntity<String> createABHAResponseEntity = hipWebClient.post(DEMOGRAPHIC_AUTH_ENDPOINT, ObjectJsonMapper.writeValueAsString(getCreateABHARequest(subject)));
            logger.info("ABHA created successfully");
            return ObjectJsonMapper.readValue(createABHAResponseEntity.getBody(), CreateABHASuccessResponse.class);
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            logger.error("ABHA creation failed with error: " + responseBody);
            CreateABHAErrorReponse errorResponse = ObjectJsonMapper.readValue(responseBody, CreateABHAErrorReponse.class);
            throw new ABHACreationFailedException(errorResponse.getDetails().get(0).getMessage());
        }
    }

    public boolean isABHAAddressLinkedAlready(String abhaAddress) {
        String existingPatientsResponseBody = openMRSWebClient.get(String.format("%s%s/%s", openmrsUrlPrefix, EXISTING_PATIENTS_ENDPOINT, abhaAddress));
        Map<String, Object> existingPatientsResponseMap = ObjectJsonMapper.readValue(existingPatientsResponseBody, Map.class);
        return existingPatientsResponseMap.get("patientUuid") != null && existingPatientsResponseMap.get("validPatient").toString().equals("true");
    }

    private CreateABHARequest getCreateABHARequest(Subject subject) {
        CreateABHARequest createABHARequest = new CreateABHARequest();
        createABHARequest.setAadhaarNumber(subject.getObservation("Aadhaar Number").toString());
        createABHARequest.setState(subject.getLocation("State").toString());
        createABHARequest.setDistrict(subject.getLocation("District").toString());
        createABHARequest.setName(sanitizeFullName(subject.getFullName()));
        createABHARequest.setGender(genderMap.get(subject.getGender()));
        createABHARequest.setMobileNumber(subject.getObservation("Phone Number").toString());
        createABHARequest.setDateOfBirth(getYearString(subject.getDateOfBirth()));
        return createABHARequest;


    }

    private String sanitizeFullName(String fullName) {
        if (fullName.endsWith(" .")) {
            return fullName.substring(0, fullName.length() - 2);
        } else {
            return fullName;
        }
    }

    private String getYearString(String date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy");
        return df.format(FormatAndParseUtil.fromAvniDate(date));


    }
}
