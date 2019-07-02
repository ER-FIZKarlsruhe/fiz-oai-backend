package de.fiz.oai.backend.service.impl;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import de.fiz.oai.backend.dao.DAOFormat;
import de.fiz.oai.backend.dao.impl.CassandraDAOFormat;
import de.fiz.oai.backend.models.Format;
import de.fiz.oai.backend.service.FormatService;

public class FormatServiceImpl implements FormatService {

  private DAOFormat daoFormat = new CassandraDAOFormat();
  
  @Override
  public Format read(String metadataPrefix) throws IOException {
    Format format = daoFormat.read(metadataPrefix);
    return format;
  }

  @Override
  public Format create(Format format) throws IOException {
    Format newFormat = daoFormat.create(format);
    return newFormat;
  }

  @Override
  public Format update(Format format) throws IOException {
    Format oldFormat = daoFormat.read(format.getMetadataPrefix());

    if (oldFormat == null) {
      throw new WebApplicationException(Status.NOT_FOUND);
    }
    
    //In Cassandra create and update are the same
    Format updatedFormat = daoFormat.create(format);
    return updatedFormat;
  }
  
  @Override
  public List<Format> readAll() throws IOException {
    List<Format> formatList = daoFormat.readAll();
    return formatList;
  }

  @Override
  public void delete(String metadataPrefix) throws IOException {
    daoFormat.delete(metadataPrefix);
  }

}
