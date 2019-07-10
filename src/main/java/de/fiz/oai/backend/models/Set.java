package de.fiz.oai.backend.models;

public class Set {

    private String name;
    
    private String spec;
    
    private String description;
    
    private String searchTerm;
    
    private String searchQuery;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpec() {
      return spec;
    }

    public void setSpec(String spec) {
      this.spec = spec;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public String getSearchTerm() {
      return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
      this.searchTerm = searchTerm;
    }

    public String getSearchQuery() {
      return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
      this.searchQuery = searchQuery;
    }


}
