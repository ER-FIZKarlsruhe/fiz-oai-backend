/*
 * Copyright 2019 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fiz.oai.backend.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class XPathHelperTest {

  private String content;

  @Before
  public void init() throws IOException {
    String fullFilePath = getClass().getClassLoader().getResource("10.1007-BF01616320.xml").getPath();
    if (fullFilePath.startsWith("/C:")) {
      fullFilePath = fullFilePath.substring(1);
    }

    content = Files.readString(Paths.get(fullFilePath));
  }

  @Test
  public void testEmptyParams() {
    assertFalse(XPathHelper.isTextValueMatching(null, null));
    assertFalse(XPathHelper.isTextValueMatching(null, ""));
    assertFalse(XPathHelper.isTextValueMatching("", null));
    assertFalse(XPathHelper.isTextValueMatching("", ""));
    assertFalse(XPathHelper.isTextValueMatching(null, "a"));
    assertFalse(XPathHelper.isTextValueMatching("a", null));
    assertFalse(XPathHelper.isTextValueMatching("", "a"));
    assertFalse(XPathHelper.isTextValueMatching("a", ""));
  }

  @Test
  public void testWrong() {
    assertFalse(XPathHelper.isTextValueMatching(content,
        "/article/front/article-meta/contrib-group/contrib/name[surname='Giulio']"));
    assertFalse(
        XPathHelper.isTextValueMatching(content, "/article/front/article-meta/contrib-group/contrib/name/nickname"));
  }

  @Test
  public void testOK() {
    assertTrue(XPathHelper.isTextValueMatching(content,
        "/article/front/article-meta/contrib-group/contrib/name[surname='Blume']"));
    assertTrue(
        XPathHelper.isTextValueMatching(content, "/article/front/article-meta/contrib-group/contrib/name/surname"));
  }

}
