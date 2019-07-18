package de.fiz.oai.backend.utils;

import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.XMLReader;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fiz.oai.backend.xsd.oai_dc.OaiDcType;

public class OaiDcHelper {

  public static String xmlToJson(String xml) {
    String json = null;

    try (StringReader reader = new StringReader(xml)) {
      System.setProperty("javax.xml.accessExternalDTD", "all");
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      spf.setValidating(false);
      SAXParser sp = spf.newSAXParser();
      XMLReader xr = sp.getXMLReader();

      JAXBContext jc = JAXBContext.newInstance(OaiDcType.class);
      Unmarshaller unmarshaller = jc.createUnmarshaller();
      UnmarshallerHandler unmarshallerHandler = unmarshaller.getUnmarshallerHandler();
      xr.setContentHandler(unmarshallerHandler);

      Source source = new StreamSource(reader);
      JAXBElement<OaiDcType> root = unmarshaller.unmarshal(source, OaiDcType.class);
      OaiDcType oaiDctype = root.getValue();

      ObjectMapper mapper = new ObjectMapper();
      json = mapper.writeValueAsString(oaiDctype);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("caused by: " + xml);
    }

    return json;
  }
}
