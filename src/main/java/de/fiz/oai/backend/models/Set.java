package de.fiz.oai.backend.models;

public class Set {

    private String name;
    private String searchUrl;
    private String identifierSelector;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSearchUrl() {
        return searchUrl;
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public String getIdentifierSelector() {
        return identifierSelector;
    }

    public void setIdentifierSelector(String identifierSelector) {
        this.identifierSelector = identifierSelector;
    }
}
