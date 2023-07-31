package org.avni_integration_service.bahmni.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResults<T> {
    private List<T> results;

    public List<T> getResults() {
        return results;
    }

    public void setResults(List<T> results) {
        this.results = results;
    }

    public SearchResults<T> removeDuplicates() {
        SearchResults<T> tSearchResults = new SearchResults<>();
        tSearchResults.setResults(this.results.stream().distinct().collect(Collectors.toList()));
        return tSearchResults;
    }
}
