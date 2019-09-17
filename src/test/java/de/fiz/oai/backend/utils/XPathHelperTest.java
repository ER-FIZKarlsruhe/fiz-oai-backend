package de.fiz.oai.backend.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class XPathHelperTest {

  private String content;
  
  @Before
  public void init() throws IOException {
    content = Files.readString(Paths.get(getClass().getClassLoader().getResource("10.1007-BF01616320.xml").getPath().substring(1)));    
  }
  
  @Test
  public void testEmptyParams() {
    assertFalse(XPathHelper.itMatches(null, null, null));
    assertFalse(XPathHelper.itMatches(null, null, ""));
    assertFalse(XPathHelper.itMatches(null, "", null));
    assertFalse(XPathHelper.itMatches(null, "", ""));
    assertFalse(XPathHelper.itMatches("", null, null));
    assertFalse(XPathHelper.itMatches("", null, ""));
    assertFalse(XPathHelper.itMatches("", "", null));
    assertFalse(XPathHelper.itMatches("", "", ""));
    assertFalse(XPathHelper.itMatches(null, null, "a"));
    assertFalse(XPathHelper.itMatches(null, "a", null));
    assertFalse(XPathHelper.itMatches(null, "a", "a"));
    assertFalse(XPathHelper.itMatches("a", null, null));
    assertFalse(XPathHelper.itMatches("a", null, "a"));
    assertFalse(XPathHelper.itMatches("a", "a", null));
  }
  
  @Test
  public void testWrongXPath() throws IOException {
    assertFalse(XPathHelper.itMatches(content, "/article/front/article-meta/contrib-group/contrib/name/surname/nonexistentnode", "Blume"));
  }
  
  @Test
  public void testWrongValue() {
    assertFalse(XPathHelper.itMatches(content, "/article/front/article-meta/contrib-group/contrib/name/surname", "Giulio"));    
  }
  
  @Test
  public void testOK() {
    assertTrue(XPathHelper.itMatches(content, "/article/front/article-meta/contrib-group/contrib/name/surname", "Blume"));    
  }
  
}
