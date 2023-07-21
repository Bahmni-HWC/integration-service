package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.avni_integration_service.util.ObjectJsonMapper;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMRSFullEncounter {
    private String uuid;
    private String encounterDatetime;
    protected List<OpenMRSEncounterProvider> encounterProviders = new ArrayList<>();
    private OpenMRSUuidHolder patient;
    private OpenMRSUuidHolder encounterType;
    private OpenMRSUuidHolder location;
    private final Map<String, Object> map = new HashMap<>();
    private boolean voided;

    private static final DecimalFormat doseFormat = new DecimalFormat("###.#");

    public OpenMRSUuidHolder getPatient() {
        return patient;
    }

    public void setPatient(OpenMRSUuidHolder patient) {
        this.patient = patient;
    }

    public OpenMRSUuidHolder getEncounterType() {
        return encounterType;
    }

    public void setEncounterType(OpenMRSUuidHolder encounterType) {
        this.encounterType = encounterType;
    }

    public OpenMRSUuidHolder getLocation() {
        return location;
    }

    public void setLocation(OpenMRSUuidHolder location) {
        this.location = location;
    }

    @JsonAnySetter
    public void setAny(String name, Object obj) {
        map.put(name, obj);
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public List<OpenMRSObservation> getLeafObservations() {
        List<Map<String, Object>> observations = (List<Map<String, Object>>) map.get("obs");
        List<OpenMRSObservation> leafObservations = new ArrayList<>();
        observations.forEach(observation -> {
            addLeafObservation(leafObservations, observation);
        });
        return leafObservations;
    }

    public Optional<OpenMRSObservation> findObservation(String conceptUuid) {
        var observations = (List<Map<String, Object>>) map.get("obs");
        return observations.stream()
                .map(this::getOpenMRSObservation)
                .filter(observation -> Objects.equals(observation.getConceptUuid(), conceptUuid))
                .findFirst();
    }

    public List<OpenMRSObservation> getLeafObservations(String form) {
        Map<String, Object> formObservationNode = findForm(form);

        List<OpenMRSObservation> leafObservations = new ArrayList<>();
        addLeafObservation(leafObservations, formObservationNode);
        return leafObservations;
    }

    private void addLeafObservation(List<OpenMRSObservation> leafObservations, Map<String, Object> observation) {
        List<Map<String, Object>> groupMembers = (List<Map<String, Object>>) observation.get("groupMembers");
        if (groupMembers != null) {
            for (Map<String, Object> groupMember : groupMembers) {
                if (groupMember.get("groupMembers") instanceof List<?>) {
                    OpenMRSObservation openMRSObservation = getOpenMRSObservationWithMultipleGroupMembers(groupMember);
                    if (!openMRSObservation.isVoided()) {
                        leafObservations.add(openMRSObservation);
                    }
                } else addLeafObservation(leafObservations, groupMember);
            }
        } else {
            OpenMRSObservation openMRSObservation = getOpenMRSObservation(observation);
            if (!openMRSObservation.isVoided())
                leafObservations.add(openMRSObservation);
        }
    }
    private OpenMRSObservation getOpenMRSObservationWithMultipleGroupMembers(Map<String, Object> observation) {
        OpenMRSObservation openMRSObservation = new OpenMRSObservation();
        Map<String, Object> conceptNode = (Map<String, Object>) observation.get("concept");
        String conceptUuid = (String) conceptNode.get("uuid");
        openMRSObservation.setConceptUuid(conceptUuid);
        openMRSObservation.setObsUuid((String) observation.get("uuid"));
        openMRSObservation.setVoided((Boolean) observation.get("voided"));
        List<OpenMRSObservation> newGroupMembers = new ArrayList<>();

        List<Map<String, Object>> groupMembers = (List<Map<String, Object>>) observation.get("groupMembers");
        if (groupMembers != null) {
            int groupMembersSize = groupMembers.size();

            for (Map<String, Object> groupMember : groupMembers) {
                OpenMRSObservation groupMemberObservationWithMultipleGroupMembers = new OpenMRSObservation();
                while (groupMembersSize > 0) {
                    if (groupMember.get("groupMembers") instanceof List<?>) {
                        getOpenMRSObservationWithMultipleGroupMembers(groupMember);
                    } else {
                        Map<String, Object> conceptNode1 = (Map<String, Object>) groupMember.get("concept");
                        String conceptUuid1 = (String) conceptNode1.get("uuid");
                        groupMemberObservationWithMultipleGroupMembers.setConceptUuid(conceptUuid1);
                        groupMemberObservationWithMultipleGroupMembers.setObsUuid((String) groupMember.get("uuid"));
                        groupMemberObservationWithMultipleGroupMembers.setVoided((Boolean) groupMember.get("voided"));
                        if (isComplexObs(groupMember)) {
                            groupMemberObservationWithMultipleGroupMembers.setValueComplex(getValueforComplexObs(groupMember));
                        } else {
                            Object value = groupMember.get("value");
                            if (value instanceof Map) {
                                value = ((Map) value).get("uuid");
                            }
                            groupMemberObservationWithMultipleGroupMembers.setValue(value);
                        }
                        newGroupMembers.add(groupMemberObservationWithMultipleGroupMembers);
                        groupMembersSize = groupMembersSize - 1;
                        break;
                    }
                }
            }
        }

        openMRSObservation.setGroupMembers(newGroupMembers);

        if (isComplexObs(observation)) {
            openMRSObservation.setValueComplex(getValueforComplexObs(observation));
        } else {
            Object value = observation.get("value");
            if (value instanceof Map) {
                value = ((Map) value).get("uuid");
            }
            openMRSObservation.setValue(value);
        }
        return openMRSObservation;
    }

    private OpenMRSObservation getOpenMRSObservation(Map<String, Object> observation) {
        OpenMRSObservation openMRSObservation = new OpenMRSObservation();
        Map<String, Object> conceptNode = (Map<String, Object>) observation.get("concept");
        String conceptUuid = (String) conceptNode.get("uuid");
        openMRSObservation.setConceptUuid(conceptUuid);
        openMRSObservation.setObsUuid((String) observation.get("uuid"));
        if(isComplexObs(observation))
        {
            openMRSObservation.setValueComplex(getValueforComplexObs(observation));
        }
        else {
            Object value = observation.get("value");
            if (value instanceof Map) {
                value = ((Map) value).get("uuid");
            }
            openMRSObservation.setValue(value);
        }
        openMRSObservation.setVoided((Boolean) observation.get("voided"));
        return openMRSObservation;
    }

    public String getUuid() {
        return uuid;
    }

    public String getEncounterDatetime() {
        return encounterDatetime;
    }

    private Map<String, Object> findForm(String uuid) {
        List<Map<String, Object>> observations = (List<Map<String, Object>>) map.get("obs");
        return observations.stream().filter(stringObjectMap -> {
            Map<String, Object> conceptObj = (Map<String, Object>) stringObjectMap.get("concept");
            return conceptObj.get("uuid").equals(uuid);
        }).findFirst().orElse(null);
    }

    public List<String> getForms() {
        List<Map<String, Object>> observations = (List<Map<String, Object>>) map.get("obs");
        return observations.stream().map(stringObjectMap -> {
            Map<String, Object> conceptObj = (Map<String, Object>) stringObjectMap.get("concept");
            return (String) conceptObj.get("uuid");
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getDrugOrderList() {
        List<Map<String, Object>> orders = (List<Map<String, Object>>) map.get("orders");
        if (orders == null || orders.size() == 0) return new ArrayList<>();
        return orders.stream().filter(stringObjectMap -> {
            Map<String, Object> orderType = (Map<String, Object>) stringObjectMap.get("orderType");
            return "Drug Order".equals(orderType.get("name"));
        }).collect(Collectors.toList());
    }

    public List<String> getDrugOrders(OpenMRSDefaultEncounter defaultEncounter) {
        List<Map<String, Object>> drugOrderList = getDrugOrderList();
        return drugOrderList.stream().filter(stringObjectMap -> defaultEncounter.isNotVoided((String) stringObjectMap.get("uuid")) && stringObjectMap.get("doseUnits") != null).map(stringObjectMap -> {
            Map<String, Object> drug = (Map<String, Object>) stringObjectMap.get("drug");
            Map<String, Object> doseUnits = (Map<String, Object>) stringObjectMap.get("doseUnits");

            String dose = stringObjectMap.get("dose") == null ? "" : doseFormat.format(stringObjectMap.get("dose"));
            int duration = (int) stringObjectMap.get("duration");
            boolean asNeeded = (boolean) stringObjectMap.get("asNeeded");
            String scheduledDate = (String) stringObjectMap.get("scheduledDate");
            Date date = FormatAndParseUtil.fromIsoDateString(scheduledDate);
            String humanReadableDate = FormatAndParseUtil.toHumanReadableFormat(date);

            return asNeeded ?
                    String.format("%s %s - as needed - starting %s", drug.get("display"), doseUnits.get("display"), humanReadableDate) :
                    String.format("%s - %s for %d days - starting %s", drug.get("display"), getDosingInstruction(stringObjectMap), duration, humanReadableDate);
        }).collect(Collectors.toList());
    }

    private String getDosingInstruction(Map<String, Object> drugOrder){
        Map<String, Object> doseUnits = (Map<String, Object>) drugOrder.get("doseUnits");
        String dose = drugOrder.get("dose") == null ? "" : doseFormat.format(drugOrder.get("dose"));
        if(drugOrder.get("frequency") != null){
            Map<String, Object> frequency = (Map<String, Object>) drugOrder.get("frequency");
            return String.format("%s %s  %s", dose, doseUnits.get("display"), frequency.get("display"));
        }
        else{
            Map<String, Object> dosingInstructions = ObjectJsonMapper.readValue((String) drugOrder.get("dosingInstructions"),Map.class);
            return String.format("%s-%s-%s %s", dosingInstructions.get("morningDose"), dosingInstructions.get("afternoonDose"), dosingInstructions.get("eveningDose"), doseUnits.get("display"));
        }


    }

    public String getVisitTypeUuid() {
        Map<String, Object> visit = (Map<String, Object>) map.get("visit");
        Map<String, Object> visitType = (Map<String, Object>) visit.get("visitType");
        return (String) visitType.get("uuid");
    }

    public boolean isVoided() {
        return voided;
    }

    public boolean hasDrugOrders() {
        return getDrugOrderList().size() != 0;
    }

    public Object getObservationValue(String conceptUuid) {
        List<OpenMRSObservation> leafObservations = getLeafObservations();
        OpenMRSObservation obs = leafObservations.stream().filter(openMRSObservation -> openMRSObservation.getConceptUuid().equals(conceptUuid)).findFirst().orElse(null);
        if (obs == null) return null;
        return obs.getValue();
    }

    //TODO: This should be handled in a better way by getting and validating against /bahmnicore/observations API
    private boolean isComplexObs(Map<String, Object> observation) {
        Object obsValue = observation.get("value");
        return obsValue instanceof Map  && ((Map) obsValue).get("display").toString().equals("raw file");
    }

    private String getValueforComplexObs(Map<String, Object> observation){
        String displayField = (String) observation.get("display");
        return displayField.split(":")[1].trim();
    }
}
