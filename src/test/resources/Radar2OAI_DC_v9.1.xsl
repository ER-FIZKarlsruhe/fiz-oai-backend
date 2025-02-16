<?xml version="1.0" encoding="UTF-8"?>
<!--
Copyright 2022 FIZ Karlsruhe - Leibniz-Institut fuer Informationsinfrastruktur GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/" xmlns:dc="http://purl.org/dc/elements/1.1/" version="1.0">

  <xsl:output method="xml" indent="yes"/>

  <!-- Used in xsl files for later versions to apply the corresponding layout per condition -->
  <xsl:template match="*">
    <xsl:for-each select="namespace::*">
      <namespace><xsl:value-of select="local-name()"/>:<xsl:value-of select="."/></namespace>
      <xsl:if test="local-name() = ''">
        <xsl:value-of select="."/>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="/">
    <!-- Check for radar version and apply the corresponding template -->
    <xsl:variable name="namespace">
      <xsl:apply-templates select="*"/>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="contains($namespace, '/v09/')">
        <xsl:call-template name="radar-v09"/>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="radar-v09">
    <oai_dc:dc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
               xmlns:dc="http://purl.org/dc/elements/1.1/"
               xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ https://www.openarchives.org/OAI/2.0/oai_dc.xsd">
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='identifier']">
        <xsl:choose>
          <xsl:when test="@identifierType = 'DOI'">
            <dc:identifier>https://dx.doi.org/<xsl:value-of select="."/></dc:identifier>
          </xsl:when>
          <xsl:otherwise>
            <dc:identifier><xsl:value-of select="."/></dc:identifier>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>

      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='creators']/*[local-name()='creator']">
        <dc:creator>
          <xsl:value-of select="*[local-name()='creatorName']"/>
        </dc:creator>

      </xsl:for-each>




      <dc:title>
        <xsl:value-of select="*[local-name()='radarDataset']/*[local-name()='title']"/>
      </dc:title>
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='publishers']">
        <dc:publisher>
          <xsl:value-of select="*[local-name()='publisher']"/>
        </dc:publisher>
      </xsl:for-each>
      <dc:date>
        <xsl:value-of select="*[local-name()='radarDataset']/*[local-name()='publicationYear']"/>
      </dc:date>
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='subjectAreas']/*[local-name()='subjectArea']/*">
        <dc:subject>
          <xsl:value-of select="."/>
        </dc:subject>
      </xsl:for-each>

      <dc:type>dataset</dc:type>

	  <xsl:if test="*[local-name()='radarDataset']/*[local-name()='resource'] != ''">
		  <dc:subject>
			<xsl:value-of select="*[local-name()='radarDataset']/*[local-name()='resource']"/>
		  </dc:subject>
	  </xsl:if>
	  
      <dc:subject>
        <xsl:value-of select="*[local-name()='radarDataset']/*[local-name()='resource']/@resourceType"/>
      </dc:subject>

      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='dataSources']/*[local-name()='dataSource']">
        <dc:source>
          <xsl:value-of select="."/>
        </dc:source>
      </xsl:for-each>
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='dataSources']/*[local-name()='dataSource']/@dataSourceDetail">
        <dc:source>
          <xsl:value-of select="."/>
        </dc:source>
      </xsl:for-each>


      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='rights']/*">
        <dc:rights>info:eu-repo/semantics/openAccess</dc:rights>

        <xsl:choose>
          <xsl:when test=".='CC BY 4.0 Attribution'">
            <dc:rights>https://creativecommons.org/licenses/by/4.0/legalcode</dc:rights>
          </xsl:when>
          <xsl:when test=".='CC BY-ND 4.0 Attribution-NoDerivs'">
            <dc:rights>https://creativecommons.org/licenses/by-nd/4.0/legalcode</dc:rights>
          </xsl:when>
          <xsl:when test=".='CC BY-SA 4.0 Attribution-ShareAlike'">
            <dc:rights>https://creativecommons.org/licenses/by-sa/4.0/legalcode</dc:rights>
          </xsl:when>
          <xsl:when test=".='CC BY-NC 4.0 Attribution-NonCommercial'">
            <dc:rights>https://creativecommons.org/licenses/by-nc/4.0/legalcode</dc:rights>
          </xsl:when>
          <xsl:when test=".='CC BY-NC-SA 4.0 Attribution-NonCommercial-ShareAlike'">
            <dc:rights>https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode</dc:rights>
          </xsl:when>
          <xsl:when test=".='CC BY-NC-ND 4.0 Attribution-NonCommercial-NoDerivs'">
            <dc:rights>https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode</dc:rights>
          </xsl:when>
          <xsl:when test=".='CC0 1.0 Universal Public Domain Dedication'">
            <dc:rights>https://creativecommons.org/publicdomain/zero/1.0/legalcode</dc:rights>
          </xsl:when>
          <xsl:when test=".='Attribution License (ODC-By)'">
            <dc:rights>https://opendatacommons.org/licenses/by/1.0/</dc:rights>
          </xsl:when>
          <!-- Is not listed in fabrica -->
          <xsl:when test=".='Open Database License (ODC-ODbL)'">
            <dc:rights>http://www.opendatacommons.org/licenses/odbl/1.0/</dc:rights>
          </xsl:when>
          <!-- Is not listed in fabrica -->
          <xsl:when test=".='Public Domain Dedication and License (PDDL)'">
            <dc:rights>http://opendatacommons.org/licenses/pddl/1.0/</dc:rights>
          </xsl:when>

          <xsl:when test=".='Apache License 2.0'">
            <dc:rights>http://www.apache.org/licenses/LICENSE-2.0</dc:rights>
          </xsl:when>
          <xsl:when test=".='Common Development and Distribution License 1.0'">
            <dc:rights>https://opensource.org/licenses/cddl1</dc:rights>
          </xsl:when>

          <xsl:when test=".='Eclipse Public License 1.0'">
            <dc:rights>http://www.eclipse.org/legal/epl-v10.html</dc:rights>
          </xsl:when>
          <xsl:when test=".='Eclipse Public License 2.0'">
            <dc:rights>https://www.eclipse.org/legal/epl-2.0</dc:rights>
          </xsl:when>
          <xsl:when test=".='GNU General Public License v3.0 only'">
            <dc:rights>https://www.gnu.org/licenses/gpl-3.0-standalone.html</dc:rights>
          </xsl:when>
          <xsl:when test=".='GNU Lesser General Public License v3.0 only'">
            <dc:rights>https://www.gnu.org/licenses/lgpl-3.0-standalone.html</dc:rights>
          </xsl:when>
          <xsl:when test=".='BSD 2-Clause Simplified License'">
            <dc:rights>https://opensource.org/licenses/BSD-2-Clause</dc:rights>
          </xsl:when>
          <xsl:when test=".='BSD 3-Clause New or Revised License'">
            <dc:rights>https://opensource.org/licenses/BSD-3-Clause</dc:rights>
          </xsl:when>
          <xsl:when test=".='MIT License'">
            <dc:rights>https://opensource.org/licenses/MIT</dc:rights>
          </xsl:when>
          <xsl:otherwise>
            <dc:rights><xsl:value-of select="."/></dc:rights>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>

      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='contributors']/*[local-name()='contributor']">
        <dc:contributor>
          <xsl:value-of select="*[local-name()='contributorName']"/>
        </dc:contributor>  
        
      </xsl:for-each>
      
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='descriptions']/*[local-name()='description']">
        <dc:description>
          <xsl:value-of select="."/>
        </dc:description>
      </xsl:for-each>
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='keywords']/*[local-name()='keyword']">
        <dc:subject>
          <xsl:value-of select="."/>
        </dc:subject>
      </xsl:for-each>
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='contributors']/*[local-name()='contributor']">
        <dc:contributor>
          <xsl:value-of select="*[local-name()='contributorName']"/>
        </dc:contributor>
      </xsl:for-each>
      
      <xsl:if test="*[local-name()='radarDataset']/*[local-name()='language']">
		  <dc:language>
			<xsl:value-of select="*[local-name()='radarDataset']/*[local-name()='language']"/>
		  </dc:language>
      </xsl:if>
      
      <xsl:if test="*[local-name()='radarDataset']/*[local-name()='alternateIdentifiers']/*[local-name()='alternateIdentifier']">
        <dc:identifier>
          <xsl:value-of select="*[local-name()='radarDataset']/*[local-name()='alternateIdentifiers']/*[local-name()='alternateIdentifier']"/>
        </dc:identifier>
        <dc:identifier>
          <xsl:value-of select="*[local-name()='radarDataset']/*[local-name()='alternateIdentifiers']/*[local-name()='alternateIdentifier']/@alternateIdentifierType"/>
        </dc:identifier>
      </xsl:if>
      
      <xsl:if test="*[local-name()='radarDataset']/*[local-name()='relatedIdentifiers']/*[local-name()='relatedIdentifier']">
        <dc:relation>
          <xsl:value-of select="*[local-name()='radarDataset']/*[local-name()='relatedIdentifiers']/*[local-name()='relatedIdentifier']"/>
        </dc:relation>
      </xsl:if>
      
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='geoLocations']/*[local-name()='geoLocation']">
        <dc:coverage>
          <xsl:choose>
            <xsl:when test="*[local-name()='geoLocationCountry'] and *[local-name()='geoLocationRegion']">
              <xsl:value-of select="*[local-name()='geoLocationCountry']"/>
              <xsl:text> (</xsl:text>
              <xsl:value-of select="*[local-name()='geoLocationRegion']"/>
              <xsl:text>)</xsl:text>
            </xsl:when>
            <xsl:when test="*[local-name()='geoLocationCountry']">
              <xsl:value-of select="*[local-name()='geoLocationCountry']"/>
            </xsl:when>
            <xsl:when test="*[local-name()='geoLocationRegion']">
              <xsl:value-of select="*[local-name()='geoLocationRegion']"/>
            </xsl:when>
          </xsl:choose>
          <xsl:if test="*[local-name()='geoLocationPoint'] or *[local-name()='geoLocationBox']">
            <xsl:text>: </xsl:text>
            <xsl:choose>
              <xsl:when test="*[local-name()='geoLocationBox']">
                <xsl:text>Lat/Long/Datum </xsl:text>
                <xsl:value-of select="*[local-name()='geoLocationBox']/*[local-name()='southWestPoint']/*[local-name()='latitude']"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="*[local-name()='geoLocationBox']/*[local-name()='southWestPoint']/*[local-name()='longitude']"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="*[local-name()='geoLocationBox']/*[local-name()='northEastPoint']/*[local-name()='latitude']"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="*[local-name()='geoLocationBox']/*[local-name()='northEastPoint']/*[local-name()='longitude']"/>
              </xsl:when>
              <xsl:when test="*[local-name()='geoLocationPoint']">
                <xsl:text>Lat/Long/Datum </xsl:text>
                <xsl:value-of select="*[local-name()='geoLocationPoint']/*[local-name()='latitude']"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="*[local-name()='geoLocationPoint']/*[local-name()='longitude']"/>
              </xsl:when>
            </xsl:choose>
            <xsl:text> (WGS 84)</xsl:text>
          </xsl:if>
        </dc:coverage>
      </xsl:for-each>
      <xsl:for-each select="*[local-name()='radarDataset']/*[local-name()='fundingReferences']/*[local-name()='fundingReference']">
        <xsl:if test="*[local-name()='funderIdentifier']">
          <dc:relation>
            <xsl:value-of select="*[local-name()='funderIdentifier']"/>
          </dc:relation>
        </xsl:if>
      </xsl:for-each>
      <dc:format>application/x-tar</dc:format>
    </oai_dc:dc>
  </xsl:template>

</xsl:stylesheet>