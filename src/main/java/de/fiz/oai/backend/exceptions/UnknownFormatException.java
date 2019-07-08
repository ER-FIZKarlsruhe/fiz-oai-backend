package de.fiz.oai.backend.exceptions;

import java.io.IOException;

public class UnknownFormatException extends IOException{

  public UnknownFormatException(String msg) {
    super(msg);
  }
  
}
