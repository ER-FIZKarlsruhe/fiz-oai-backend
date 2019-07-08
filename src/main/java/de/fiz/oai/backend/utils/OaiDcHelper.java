package de.fiz.oai.backend.utils;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import de.fiz.oai.backend.xsd.oai_dc.OaiDcType;


public class OaiDcHelper {

  public static String xmlToJson(String xml) {
    String json = null;
    try
    {
      
      JAXBContext jaxbContext = JAXBContext.newInstance(OaiDcType.class);

      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      //OaiDcType oaiDctype = (OaiDcType) jaxbUnmarshaller.unmarshal(new StringReader(xml));
    
      Source source = new StreamSource(new StringReader(xml));
      JAXBElement<OaiDcType> root = unmarshaller.unmarshal(source, OaiDcType.class);
      OaiDcType oaiDctype = root.getValue();
      
      
      ObjectMapper mapper = new ObjectMapper();  
     // AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(mapper.getTypeFactory());
     // mapper.setAnnotationIntrospector(introspector);

      json = mapper.writeValueAsString(oaiDctype);
        
      System.out.println("*** Converting XML to JSON ***");
      System.out.println(json);
        

    } catch (IOException e)
    {
        e.printStackTrace();
    } catch (JAXBException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return json;
  }
}
