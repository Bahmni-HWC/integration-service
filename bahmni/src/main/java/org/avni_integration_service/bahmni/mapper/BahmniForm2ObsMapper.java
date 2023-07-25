package org.avni_integration_service.bahmni.mapper;

import org.apache.log4j.Logger;
import org.avni_integration_service.bahmni.BahmniMappingGroup;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BahmniForm2ObsMapper {

    private final MappingMetaDataRepository mappingMetaDataRepository;

    private final BahmniMappingGroup bahmniMappingGroup;

    private final BahmniMappingType bahmniMappingType;

    private static final Logger logger = Logger.getLogger(BahmniForm2ObsMapper.class);

    public BahmniForm2ObsMapper(MappingMetaDataRepository mappingMetaDataRepository, BahmniMappingGroup bahmniMappingGroup, BahmniMappingType bahmniMappingType) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
    }

    public Map<String, Object> mapForm2ObsToAvniObs(List<Map<String, Object>> form2Obs) {
        Map<String, Object> avniObservations = new HashMap<>();
        form2Obs.forEach(form2Ob -> {
            String conceptUuid = getConceptUuid((Map<String, Object>) form2Ob.get("concept"));
            String avniConceptName = getAvniConceptNameForOpenMRSConcept(conceptUuid);
            if (avniConceptName == null) {
                logger.info("No concept mapping defined for OpenMRS concept uuid: " + conceptUuid);
                return;
            }
            Object avniObsValue = mapObsValueToAvniValue(form2Ob);
            avniObservations.compute(avniConceptName, (key, value) -> {
                if (value instanceof List) { //Append to the existing list
                    ((List<Object>) value).add(avniObsValue);
                    return value;
                }
                else if(value != null){ //Create a list and add the existing value and the new value
                    List<Object> values = new ArrayList<>();
                    values.add(value);
                    values.add(avniObsValue);
                    return values;
                }
                return avniObsValue; //return the same value
            });


        });
        return avniObservations;
    }

    private String getConceptUuid(Map<String, Object> conceptNode) {
        return (String) conceptNode.get("uuid");
    }

    private String getAvniConceptNameForOpenMRSConcept(String conceptUuid) {
        MappingMetaData mappingMetaData = mappingMetaDataRepository.findByMappingGroupAndMappingTypeAndIntSystemValue(bahmniMappingGroup.observation, bahmniMappingType.concept, conceptUuid);
        if (mappingMetaData == null) {
            return null;
        }
        return mappingMetaData.getAvniValue();
    }

    private Object mapObsValueToAvniValue(Map<String, Object> openmrsObs) {
        Object value = openmrsObs.get("value");
        List<Map<String, Object>> groupMembers = (List<Map<String, Object>>) openmrsObs.get("groupMembers");
        if (value != null && groupMembers == null) {
            return getValueDisplayFromValueNode(value);
        } else if (groupMembers != null && value == null) {
            return mapGroupMembersToAvniQuestionGroup(groupMembers);
        }
        else if(groupMembers != null && value != null){
            logger.info("Both value and groupMembers are not null. This is not expected. Value: " + value + " groupMembers: " + groupMembers);
        }
        else if(groupMembers == null && value == null){
            logger.info("Both value and groupMembers are null. This is not expected.");
        }
        return null;

    }

    private Object mapGroupMembersToAvniQuestionGroup(List<Map<String, Object>> groupMembers) {
        Map<String, Object> avniQuestionGroup = new HashMap<>();
        groupMembers.forEach(groupMember -> {
            String conceptUuid = getConceptUuid((Map<String, Object>) groupMember.get("concept"));
            String avniConceptName = getAvniConceptNameForOpenMRSConcept(conceptUuid);
            if (avniConceptName == null) {
                logger.info("No concept mapping defined for OpenMRS concept uuid: " + conceptUuid);
                return;
            }
            Object value = groupMember.get("value");
            Object nestedGroupMembers = groupMember.get("groupMembers");
            if (value != null && nestedGroupMembers == null) {
                avniQuestionGroup.put(avniConceptName, getValueDisplayFromValueNode(value));
            } else if (nestedGroupMembers != null && value == null) {
                logger.info("Nested group members found for concept: " + conceptUuid + ". This is not implemented. " );
            }
            else if(nestedGroupMembers != null && value != null){
                logger.info("Both value and nestedGroupMembers are not null. This is not expected. ");
            }
            else if(nestedGroupMembers == null && value == null){
                logger.info("Both value and groupMembers are null. This is not expected.");
            }
        });
        return avniQuestionGroup;
    }

    private String getValueDisplayFromValueNode(Object valueNode) {
        if (valueNode instanceof Map) {
            return ((Map) valueNode).get("display").toString();
        } else {
            return valueNode.toString();
        }
    }
}
