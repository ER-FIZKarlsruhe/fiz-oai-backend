package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fiz.oai.backend.dao.DAOSet;
import de.fiz.oai.backend.models.Set;
import de.fiz.oai.backend.service.SearchService;
import de.fiz.oai.backend.service.SetService;

@Service
public class SetServiceImpl implements SetService {

  private Logger LOGGER = LoggerFactory.getLogger(SetServiceImpl.class);
  
  @Inject
  DAOSet daoSet;

  @Inject
  SearchService searchService;

  @Override
  public Set read(String name) throws IOException {
    Set set = daoSet.read(name);

    return set;
  }

  @Override
  public Set create(Set set) throws IOException {
    daoSet.create(set);

    LOGGER.info("Creating Set " + set.getName() + ". Triggering complete reindexing.");
    
    searchService.reindexAll();

    return set;
  }

  @Override
  public Set update(Set set) throws IOException {
    Set oldSet = daoSet.read(set.getName());

    if (oldSet == null) {
      throw new NotFoundException();
    }
    daoSet.create(set);

    LOGGER.info("Updating Set " + set.getName() + ". Triggering complete reindexing.");
    
    searchService.reindexAll();

    return set;
  }

  @Override
  public List<Set> readAll() throws IOException {
    final List<Set> setList = daoSet.readAll();
    return setList;
  }

  @Override
  public void delete(String name) throws IOException {
    daoSet.delete(name);

    LOGGER.info("Set " + name + " deleted. Triggering complete reindexing.");
    
    searchService.reindexAll();
  }

}
