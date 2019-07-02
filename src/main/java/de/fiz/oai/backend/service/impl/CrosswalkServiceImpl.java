package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.util.List;

import de.fiz.oai.backend.dao.DAOCrosswalk;
import de.fiz.oai.backend.dao.impl.CassandraDAOCrosswalk;
import de.fiz.oai.backend.models.Crosswalk;
import de.fiz.oai.backend.service.CrosswalkService;

public class CrosswalkServiceImpl implements CrosswalkService {

  DAOCrosswalk daoCrosswalk = new CassandraDAOCrosswalk(); 
  
  @Override
  public Crosswalk read(String name) throws IOException {
    Crosswalk crosswalk = daoCrosswalk.read(name);
    return crosswalk;
  }

  @Override
  public Crosswalk create(Crosswalk content) throws IOException {
    // TODO add more validations
    // Does the format (referenced by formatFrom) exists?
    // Does the format (referenced by formatTo) exists?
    
    Crosswalk newCrosswalk = daoCrosswalk.create(content);
    return newCrosswalk;
  }

  @Override
  public Crosswalk update(Crosswalk content) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Crosswalk> readAll() throws IOException {
    List<Crosswalk> crosswalks = daoCrosswalk.readAll();
    
    return crosswalks;
  }

  @Override
  public void delete(String name) throws IOException {
    daoCrosswalk.delete(name);
  }

}
