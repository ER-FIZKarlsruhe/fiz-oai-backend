package de.fiz.oai.backend.exceptions;

import java.io.IOException;

public class FormatValidationException extends IOException{

  public FormatValidationException(String msg) {
    super(msg);
  }
  
}
