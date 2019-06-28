package de.fiz.oai.backend.exceptions;

import java.io.IOException;

public class NotFoundException extends IOException{

  public NotFoundException(String msg) {
    super(msg);
  }
  
}
