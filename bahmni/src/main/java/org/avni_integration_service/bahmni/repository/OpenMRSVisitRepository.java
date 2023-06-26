package org.avni_integration_service.bahmni.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import org.avni_integration_service.bahmni.client.OpenMRSWebClient;
import org.avni_integration_service.bahmni.contract.OpenMRSSaveVisit;
import org.avni_integration_service.bahmni.contract.OpenMRSVisit;
import org.avni_integration_service.bahmni.contract.SearchResults;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OpenMRSVisitRepository extends BaseOpenMRSRepository {

    @Autowired
    public OpenMRSVisitRepository(OpenMRSWebClient openMRSWebClient) {
        super(openMRSWebClient);
    }

    public OpenMRSVisit getVisit(String patientUuid, String locationUuid, String visitTypeUuid, Date date) {
        String json = openMRSWebClient.get("%s?patient=%s&location=%s&v=full&fromStartDate=%s".formatted(resourcePath(), patientUuid, locationUuid, FormatAndParseUtil.toISODateString(date)));
        SearchResults<OpenMRSVisit> searchResults = ObjectJsonMapper.readValue(json, new TypeReference<SearchResults<OpenMRSVisit>>() {
        });
        var filteredByVisitType = new SearchResults<OpenMRSVisit>();
        filteredByVisitType.setResults(searchResults.getResults().stream()
                .filter(visit -> (visit.getVisitType().getUuid().equals(visitTypeUuid) && isVisitForGivenDate(visit, date)))
                .collect(Collectors.toList()));
        return filteredByVisitType.getResults().size()>0?filteredByVisitType.getResults().get(0):null;
    }

    public List<OpenMRSVisit> getVisits(String patientUuid, String locationUuid, String visitTypeUuid) {
        String json = openMRSWebClient.get("%s?patient=%s&location=%s&v=full".formatted(resourcePath(), patientUuid, locationUuid));
        SearchResults<OpenMRSVisit> searchResults = ObjectJsonMapper.readValue(json, new TypeReference<SearchResults<OpenMRSVisit>>() {
        });
        return searchResults.getResults().stream()
                .filter(visit -> visit.getVisitType().getUuid().equals(visitTypeUuid))
                .collect(Collectors.toList());
    }

    public OpenMRSVisit createVisit(OpenMRSSaveVisit openMRSSaveVisit) {
        String json = ObjectJsonMapper.writeValueAsString(openMRSSaveVisit);
        String outputJson = openMRSWebClient.post(resourcePath(), json);
        return ObjectJsonMapper.readValue(outputJson, OpenMRSVisit.class);
    }

    public void deleteVisit(String visitUuid) {
        openMRSWebClient.delete(URI.create("%s/%s?purge=true".formatted(resourcePath(), visitUuid)));
    }

    private String resourcePath() {
        return getResourcePath("visit");
    }

    private boolean isVisitForGivenDate(OpenMRSVisit visit, Date date){
        Date visitStartDateTime = visit.getStartDatetime();
        return date.after(visitStartDateTime) && date.before(FormatAndParseUtil.getEndOfDayDate(date));
    }

    public void updateVisitStopDateTime(String visitUuid, Date stopDate) {
        String json = "{\"stopDatetime\": \"%s\"}".formatted(FormatAndParseUtil.toISODateStringWithTimezone(stopDate));
        openMRSWebClient.post(getSingleResourcePath("visit",visitUuid), json);
    }

}
