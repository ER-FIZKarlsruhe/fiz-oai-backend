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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns="http://datacite.org/schema/kernel-4" xmlns:rtype="http://radar-service.eu/schemas/descriptive/radar/v09/radar-types"
                xmlns:rad="http://radar-service.eu/schemas/descriptive/radar/v09/radar-dataset" xmlns:re="http://radar-service.eu/schemas/descriptive/radar/v09/radar-elements"
                xmlns:dct="http://purl.org/dc/terms/" xmlns:dck="http://datacite.org/schema/kernel-4" xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl"
                exclude-result-prefixes="#default rtype rad re dct dck xd" version="1.0">

  <xsl:output method="xml" encoding="UTF-8" indent="yes" />

  <xsl:param name="datasetSize" />
  <xsl:param name="currentYear" />

  <xsl:template match="rad:radarDataset">
    <resource xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://datacite.org/schema/kernel-4"
              xsi:schemaLocation="http://datacite.org/schema/kernel-4 https://schema.datacite.org/meta/kernel-4.4/metadata.xsd">
      <xsl:apply-templates  select="re:identifier" />
      <xsl:apply-templates select="re:creators" />
      <xsl:call-template name="titles" />
      <xsl:apply-templates select="re:publishers" />
      <xsl:apply-templates select="re:productionYear" />
      <xsl:call-template name="publicationYear" />
      <xsl:call-template name="subjects" />
      <xsl:apply-templates select="re:resource" />
      <xsl:apply-templates select="re:rights" />
      <xsl:call-template name="contributors" />
      <xsl:call-template name="descriptions" />
      <xsl:apply-templates select="re:language" />
      <xsl:apply-templates select="re:alternateIdentifiers" />
      <xsl:apply-templates select="re:relatedIdentifiers" />
      <xsl:apply-templates select="re:geoLocations" />
      <xsl:apply-templates select="re:fundingReferences" />
      <xsl:apply-templates select="re:relatedItem" />
      <xsl:call-template name="sizes" />
      <xsl:call-template name="formats" />
    </resource>
  </xsl:template>

  <xsl:template match="re:identifier">
    <xsl:if test="@identifierType = 'DOI'">
      <identifier>
        <xsl:attribute name="identifierType"><xsl:value-of select="@identifierType" /></xsl:attribute>
        <xsl:value-of select="." />
      </identifier>
    </xsl:if>
  </xsl:template>

  <xsl:template name="nameIdentifierFromElement">
    <xsl:for-each select="re:nameIdentifier">
      <nameIdentifier>
        <xsl:if test="string-length(@nameIdentifierScheme) &gt; 0 ">
          <xsl:attribute name="nameIdentifierScheme"><xsl:value-of select="@nameIdentifierScheme" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="string-length(@schemeURI) &gt; 0 ">
          <xsl:attribute name="schemeURI"><xsl:value-of select="@schemeURI" /></xsl:attribute>
        </xsl:if>
        <xsl:value-of select="." />
      </nameIdentifier>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="nameIdentifierFromAttribute">
    <xsl:if test="string-length(@nameIdentifier) &gt; 0 ">
      <nameIdentifier>
        <xsl:if test="string-length(@nameIdentifierScheme) &gt; 0 ">
          <xsl:attribute name="nameIdentifierScheme"><xsl:value-of select="@nameIdentifierScheme" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="string-length(@schemeURI) &gt; 0 ">
          <xsl:attribute name="schemeURI"><xsl:value-of select="@schemeURI" /></xsl:attribute>
        </xsl:if>
        <xsl:value-of select="@nameIdentifier" />
      </nameIdentifier>
    </xsl:if>
  </xsl:template>


  <xsl:template name="creatorAffiliation">
    <affiliation>
      <xsl:if test="string-length(re:creatorAffiliation/@affiliationIdentifier) &gt; 0 ">
        <xsl:attribute name="affiliationIdentifier"><xsl:value-of select="re:creatorAffiliation/@affiliationIdentifier" /></xsl:attribute>

        <xsl:if test="string-length(re:creatorAffiliation/@affiliationIdentifierScheme) &gt; 0 ">
          <xsl:attribute name="affiliationIdentifierScheme"><xsl:value-of select="re:creatorAffiliation/@affiliationIdentifierScheme" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="string-length(re:creatorAffiliation/@schemeURI) &gt; 0 ">
          <xsl:attribute name="schemeURI"><xsl:value-of select="re:creatorAffiliation/@schemeURI" /></xsl:attribute>
        </xsl:if>
      </xsl:if>
      <xsl:value-of select="re:creatorAffiliation" />
    </affiliation>
  </xsl:template>

  <xsl:template name="contributorAffiliation">
    <affiliation>
      <xsl:if test="string-length(re:contributorAffiliation/@affiliationIdentifier) &gt; 0 ">
        <xsl:attribute name="affiliationIdentifier"><xsl:value-of select="re:contributorAffiliation/@affiliationIdentifier" /></xsl:attribute>

        <xsl:if test="string-length(re:contributorAffiliation/@affiliationIdentifierScheme) &gt; 0 ">
          <xsl:attribute name="affiliationIdentifierScheme"><xsl:value-of select="re:contributorAffiliation/@affiliationIdentifierScheme" /></xsl:attribute>
        </xsl:if>
        <xsl:if test="string-length(re:contributorAffiliation/@schemeURI) &gt; 0 ">
          <xsl:attribute name="schemeURI"><xsl:value-of select="re:contributorAffiliation/@schemeURI" /></xsl:attribute>
        </xsl:if>
      </xsl:if>
      <xsl:value-of select="re:contributorAffiliation" />
    </affiliation>
  </xsl:template>

  <xsl:template match="re:creators">
    <creators>
      <xsl:for-each select="re:creator">

        <creator>
          <creatorName>
            <xsl:value-of select="re:creatorName" />
          </creatorName>
          <xsl:for-each select="re:givenName">
            <givenName>
              <xsl:value-of select="." />
            </givenName>
          </xsl:for-each>
          <xsl:for-each select="re:familyName">
            <familyName>
              <xsl:value-of select="." />
            </familyName>
          </xsl:for-each>
          <xsl:call-template name="nameIdentifierFromElement" />
          <xsl:call-template name="creatorAffiliation" />

        </creator>
      </xsl:for-each>
    </creators>
  </xsl:template>

  <xsl:template name="titles">
    <titles>
      <xsl:call-template name="title" />
      <xsl:call-template name="additionalTitle" />
    </titles>
  </xsl:template>

  <xsl:template name="title">
    <title>
      <xsl:value-of select="/rad:radarDataset/re:title" />
    </title>
  </xsl:template>

  <xsl:template name="additionalTitle">
    <xsl:for-each select="/rad:radarDataset/re:additionalTitles/re:additionalTitle">
      <title>
        <xsl:attribute name="titleType"><xsl:value-of select="@additionalTitleType" /></xsl:attribute>
        <xsl:value-of select="." />
      </title>
    </xsl:for-each>
  </xsl:template>


  <xsl:template match="re:publishers">
    <publisher>
      <xsl:for-each select="re:publisher">
        <xsl:value-of select="concat(., substring(',', 2 - (position() != last())))" />
      </xsl:for-each>
    </publisher>
  </xsl:template>

  <xsl:template match="re:productionYear">
    <dates>
      <date>
        <xsl:attribute name="dateType">Created</xsl:attribute>
        <xsl:value-of select="." />
      </date>
    </dates>
  </xsl:template>

  <xsl:template name="publicationYear">
    <publicationYear>
      <xsl:choose>
        <xsl:when test="/rad:radarDataset/re:publicationYear">
          <xsl:value-of select="/rad:radarDataset/re:publicationYear" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$currentYear" />
        </xsl:otherwise>
      </xsl:choose>
    </publicationYear>
  </xsl:template>

  <xsl:template name="subjects">
    <subjects>
      <xsl:apply-templates select="re:subjectAreas" />
      <xsl:apply-templates select="re:keywords" />
    </subjects>
  </xsl:template>

  <xsl:template match="re:subjectAreas">
    <xsl:for-each select="re:subjectArea">
      <subject>
        <xsl:choose>
          <xsl:when test="re:controlledSubjectAreaName != 'Other'">
            <xsl:value-of select="re:controlledSubjectAreaName" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="re:additionalSubjectAreaName" />
          </xsl:otherwise>
        </xsl:choose>
      </subject>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="re:keywords">
    <xsl:for-each select="re:keyword">
      <subject>
        <xsl:if test="string-length(@valueURI) &gt; 0 ">
          <xsl:attribute name="valueURI"><xsl:value-of select="@valueURI" /></xsl:attribute>

          <xsl:if test="string-length(@keywordScheme) &gt; 0 ">
            <xsl:attribute name="subjectScheme"><xsl:value-of select="@keywordScheme" /></xsl:attribute>
          </xsl:if>
          <xsl:if test="string-length(@schemeURI) &gt; 0 ">
            <xsl:attribute name="schemeURI"><xsl:value-of select="@schemeURI" /></xsl:attribute>
          </xsl:if>
          <xsl:if test="string-length(@classificationCode) &gt; 0 ">
            <xsl:attribute name="classificationCode"><xsl:value-of select="@classificationCode" /></xsl:attribute>
          </xsl:if>
        </xsl:if>
        <xsl:value-of select="." />
      </subject>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="re:resource">
    <resourceType>
      <xsl:attribute name="resourceTypeGeneral">
        <xsl:value-of select="@resourceType" />
      </xsl:attribute>
      <xsl:value-of select="." />
    </resourceType>
  </xsl:template>

  <xsl:template match="re:rights">
    <xsl:for-each select="/rad:radarDataset/re:rights/re:controlledRights">
      <rightsList>
                <rights rightsURI="info:eu-repo/semantics/openAccess">Open Access</rights>

			<xsl:choose>
			  <xsl:when test=".='CC BY 4.0 Attribution'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="CC-BY-4.0" rightsURI='https://creativecommons.org/licenses/by/4.0/legalcode'>Creative Commons Attribution 4.0 International</rights>
			  </xsl:when>
			  <xsl:when test=".='CC BY-ND 4.0 Attribution-NoDerivs'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="CC-BY-ND-4.0" rightsURI='https://creativecommons.org/licenses/by-nd/4.0/legalcode'>Creative Commons Attribution No Derivatives 4.0 International</rights>
			  </xsl:when>
			  <xsl:when test=".='CC BY-SA 4.0 Attribution-ShareAlike'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="CC-BY-SA-4.0" rightsURI='https://creativecommons.org/licenses/by-sa/4.0/legalcode'>Creative Commons Attribution Share Alike 4.0 International</rights>
			  </xsl:when>
			  <xsl:when test=".='CC BY-NC 4.0 Attribution-NonCommercial'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="CC-BY-NC-4.0" rightsURI='https://creativecommons.org/licenses/by-nc/4.0/legalcode'>Creative Commons Attribution Non Commercial 4.0 International</rights>
			  </xsl:when>
			  <xsl:when test=".='CC BY-NC-SA 4.0 Attribution-NonCommercial-ShareAlike'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="CC-BY-NC-SA-4.0" rightsURI='https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode'>Creative Commons Attribution Non Commercial Share Alike 4.0 International</rights>
			  </xsl:when>
			  <xsl:when test=".='CC BY-NC-ND 4.0 Attribution-NonCommercial-NoDerivs'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="CC-BY-NC-ND-4.0" rightsURI='https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode'>Creative Commons Attribution Non Commercial No Derivatives 4.0 International</rights>
			  </xsl:when>
			  <xsl:when test=".='CC0 1.0 Universal Public Domain Dedication'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="CC0-1.0" rightsURI='https://creativecommons.org/publicdomain/zero/1.0/legalcode'>Creative Commons Zero v1.0 Universal</rights>
			  </xsl:when>
			  <xsl:when test=".='Attribution License (ODC-By)'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="ODC-By-1.0" rightsURI='https://opendatacommons.org/licenses/by/1.0/'>Open Data Commons Attribution License v1.0</rights>
			  </xsl:when>
			  <!-- Is not listed in fabrica -->
			  <xsl:when test=".='Open Database License (ODC-ODbL)'">
                <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="ODbL-1.0" rightsURI='http://www.opendatacommons.org/licenses/odbl/1.0/'>Open Data Commons Open Database License v1.0</rights>
			  </xsl:when>
				<!-- Is not listed in fabrica -->
			  <xsl:when test=".='Public Domain Dedication and License (PDDL)'">
                <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="PDDL-1.0" rightsURI='http://opendatacommons.org/licenses/pddl/1.0/'>Open Data Commons Public Domain Dedication &amp; License 1.0</rights>
			  </xsl:when>

			  <xsl:when test=".='Apache License 2.0'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="Apache-2.0" rightsURI='http://www.apache.org/licenses/LICENSE-2.0'>Apache License 2.0</rights>
			  </xsl:when>
			  <xsl:when test=".='Common Development and Distribution License 1.0'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="CDDL-1.0" rightsURI='https://opensource.org/licenses/cddl1'>Common Development and Distribution License 1.0</rights>
			  </xsl:when>

			  <xsl:when test=".='Eclipse Public License 1.0'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="EPL-1.0" rightsURI='http://www.eclipse.org/legal/epl-v10.html'>Eclipse Public License 1.0</rights>
			  </xsl:when>
			  <xsl:when test=".='Eclipse Public License 2.0'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="EPL-2.0" rightsURI='https://www.eclipse.org/legal/epl-2.0'>Eclipse Public License 2.0</rights>
			  </xsl:when>
			  <xsl:when test=".='GNU General Public License v3.0 only'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="GPL-3.0-only" rightsURI='https://www.gnu.org/licenses/gpl-3.0-standalone.html'>GNU General Public License v3.0 only</rights>
			  </xsl:when>
			  <xsl:when test=".='GNU Lesser General Public License v3.0 only'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="LGPL-3.0-only" rightsURI='https://www.gnu.org/licenses/lgpl-3.0-standalone.html'>GNU Lesser General Public License v3.0 only</rights>
			  </xsl:when>
			  <xsl:when test=".='BSD 2-Clause Simplified License'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="BSD-2-Clause" rightsURI='https://opensource.org/licenses/BSD-2-Clause'>BSD 2-Clause "Simplified" License</rights>
			  </xsl:when>
			  <xsl:when test=".='BSD 3-Clause New or Revised License'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="BSD-3-Clause" rightsURI='https://opensource.org/licenses/BSD-3-Clause'>BSD 3-Clause "New" or "Revised" License</rights>
			  </xsl:when>
			  <xsl:when test=".='MIT License'">
			    <rights schemeURI="https://spdx.org/licenses/" rightsIdentifierScheme="SPDX" rightsIdentifier="MIT" rightsURI='https://opensource.org/licenses/MIT'>MIT License</rights>
			  </xsl:when>
			  <xsl:otherwise>
				<rights><xsl:value-of select="."/></rights>
			  </xsl:otherwise>
			</xsl:choose>
      </rightsList>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="contributors">
    <contributors>
      <xsl:call-template name="rightsHolder" />
      <xsl:call-template name="contributor" />
    </contributors>
  </xsl:template>

  <xsl:template name="contributor">
    <xsl:for-each select="/rad:radarDataset/re:contributors/re:contributor">
      <contributor>
        <xsl:attribute name="contributorType"><xsl:value-of select="@contributorType" /></xsl:attribute>
        <contributorName>
          <xsl:value-of select="./re:contributorName" />
        </contributorName>
        <xsl:call-template name="nameIdentifierFromElement" />
        <xsl:call-template name="contributorAffiliation" />
      </contributor>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="rightsHolder">
    <xsl:for-each select="/rad:radarDataset/re:rightsHolders/re:rightsHolder">
      <contributor>
        <xsl:attribute name="contributorType">RightsHolder</xsl:attribute>
        <contributorName>
          <xsl:value-of select="." />
        </contributorName>
        <xsl:call-template name="nameIdentifierFromAttribute" />
      </contributor>
    </xsl:for-each>
  </xsl:template>


  <xsl:template name="descriptions">
    <descriptions>
      <xsl:call-template name="description" />
      <xsl:call-template name="dataSource" />
      <xsl:call-template name="processing" />
      <xsl:call-template name="relatedInformation" />
    </descriptions>
  </xsl:template>

  <xsl:template name="description">
    <xsl:for-each select="/rad:radarDataset/re:descriptions/re:description">
      <description>
        <xsl:choose>
          <xsl:when test="@descriptionType = 'TechnicalRemarks'">
            <xsl:attribute name="descriptionType">TechnicalInfo</xsl:attribute>
          </xsl:when>
          <xsl:when test="@descriptionType = 'Object'">
            <xsl:attribute name="descriptionType">Other</xsl:attribute>
          </xsl:when>
          <xsl:when test="@descriptionType = 'Method'">
            <xsl:attribute name="descriptionType">Methods</xsl:attribute>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="descriptionType"><xsl:value-of select="@descriptionType" /></xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:value-of select="." />
      </description>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="dataSource">
    <xsl:for-each select="/rad:radarDataset/re:dataSources/re:dataSource">
      <description>
        <xsl:choose>
          <xsl:when test="@dataSourceDetail = 'Instrument'">
            <xsl:attribute name="descriptionType">TechnicalInfo</xsl:attribute>
          </xsl:when>
          <xsl:when test="@dataSourceDetail = 'Observation'">
            <xsl:attribute name="descriptionType">Methods</xsl:attribute>
          </xsl:when>
          <xsl:when test="@dataSourceDetail = 'Organism'">
            <xsl:attribute name="descriptionType">TechnicalInfo</xsl:attribute>
          </xsl:when>
          <xsl:when test="@dataSourceDetail = 'Tissue'">
            <xsl:attribute name="descriptionType">TechnicalInfo</xsl:attribute>
          </xsl:when>
          <xsl:when test="@dataSourceDetail = 'Survey'">
            <xsl:attribute name="descriptionType">Methods</xsl:attribute>
          </xsl:when>
          <xsl:when test="@dataSourceDetail = 'Trial'">
            <xsl:attribute name="descriptionType">Methods</xsl:attribute>
          </xsl:when>
          <xsl:when test="@dataSourceDetail = 'Media'">
            <xsl:attribute name="descriptionType">TechnicalInfo</xsl:attribute>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="descriptionType"><xsl:value-of select="@dataSourceDetail" /></xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:value-of select="." />
      </description>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="processing">
    <xsl:for-each select="/rad:radarDataset/re:processing/re:dataProcessing">
      <description>
        <xsl:attribute name="descriptionType">Other</xsl:attribute>
        <xsl:value-of select="." />
      </description>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="relatedInformation">
    <xsl:for-each select="/rad:radarDataset/re:relatedInformations/re:relatedInformation">
      <description>
        <xsl:attribute name="descriptionType">Other</xsl:attribute>
        <xsl:value-of select="." />
      </description>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="re:alternateIdentifiers">
    <alternateIdentifiers>
      <xsl:for-each select="re:alternateIdentifier">
        <alternateIdentifier>
          <xsl:attribute name="alternateIdentifierType">
            <xsl:value-of select="@alternateIdentifierType" />
          </xsl:attribute>
          <xsl:value-of select="." />
        </alternateIdentifier>
      </xsl:for-each>
    </alternateIdentifiers>
  </xsl:template>


  <xsl:template match="re:relatedIdentifiers">
    <relatedIdentifiers>
      <xsl:for-each select="re:relatedIdentifier">
        <relatedIdentifier>
          <xsl:choose>
            <xsl:when  test="@relatedIdentifierType = 'ePIC'">
              <xsl:attribute name="relatedIdentifierType">Handle</xsl:attribute>
            </xsl:when >
            <xsl:otherwise>
              <xsl:attribute name="relatedIdentifierType">
                <xsl:value-of select="@relatedIdentifierType" />
              </xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:attribute name="relationType">
            <xsl:value-of select="@relationType" />
          </xsl:attribute>
          <xsl:value-of select="." />
        </relatedIdentifier>
      </xsl:for-each>
    </relatedIdentifiers>
  </xsl:template>

  <xsl:template match="re:geoLocations">
    <geoLocations>
      <xsl:for-each select="re:geoLocation">
        <geoLocation>
          <xsl:apply-templates select="re:geoLocationCountry" />
          <xsl:apply-templates select="re:geoLocationPoint" />
          <xsl:apply-templates select="re:geoLocationBox" />
        </geoLocation>
      </xsl:for-each>
    </geoLocations>
  </xsl:template>

  <xsl:template match="re:geoLocationCountry">
    <geoLocationPlace>
      <xsl:value-of select="." />
    </geoLocationPlace>
  </xsl:template>

  <xsl:template match="re:geoLocationPoint">
    <geoLocationPoint>
      <pointLongitude>
        <xsl:value-of select="re:longitude" />
      </pointLongitude>
      <pointLatitude>
        <xsl:value-of select="re:latitude" />
      </pointLatitude>
    </geoLocationPoint>
  </xsl:template>

  <xsl:template match="re:geoLocationBox">
    <geoLocationBox>
      <westBoundLongitude>
        <xsl:value-of select="re:southWestPoint/re:longitude" />
      </westBoundLongitude>
      <eastBoundLongitude>
        <xsl:value-of select="re:northEastPoint/re:longitude" />
      </eastBoundLongitude>
      <southBoundLatitude>
        <xsl:value-of select="re:southWestPoint/re:latitude" />
      </southBoundLatitude>
      <northBoundLatitude>
        <xsl:value-of select="re:northEastPoint/re:latitude" />
      </northBoundLatitude>
    </geoLocationBox>
  </xsl:template>

  <xsl:template match="re:fundingReferences">
    <fundingReferences>
      <xsl:for-each select="re:fundingReference">
        <fundingReference>
          <funderName>
            <xsl:value-of select="re:funderName" />
          </funderName>

          <xsl:if test="re:funderIdentifier != ''">
            <funderIdentifier>
              <xsl:choose>
                <xsl:when test="re:funderIdentifier/@type = 'CrossRefFunder'">
                  <xsl:attribute name="funderIdentifierType">Crossref Funder ID</xsl:attribute>
                </xsl:when>
                <xsl:when test="re:funderIdentifier/@type = 'GRID'">
                  <xsl:attribute name="funderIdentifierType">GRID</xsl:attribute>
                </xsl:when>
                <xsl:when test="re:funderIdentifier/@type = 'ISNI'">
                  <xsl:attribute name="funderIdentifierType">ISNI</xsl:attribute>
                </xsl:when>
                <xsl:when test="re:funderIdentifier/@type = 'ROR'">
                  <xsl:attribute name="funderIdentifierType">ROR</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:attribute name="funderIdentifierType">Other</xsl:attribute>
                </xsl:otherwise>
              </xsl:choose>

              <xsl:if test="string-length(re:funderIdentifier/@schemeURI) &gt; 0 ">
                <xsl:attribute name="schemeURI"><xsl:value-of select="re:funderIdentifier/@schemeURI" /></xsl:attribute>
              </xsl:if>

              <xsl:value-of select="re:funderIdentifier" />
            </funderIdentifier>
          </xsl:if>

          <xsl:if test="re:awardNumber != ''">
            <awardNumber>
              <xsl:attribute name="awardURI">
                <xsl:value-of select="re:awardURI" />
              </xsl:attribute>
              <xsl:value-of select="re:awardNumber" />
            </awardNumber>
          </xsl:if>

          <xsl:if test="re:awardTitle != ''">
            <awardTitle>
              <xsl:value-of select="re:awardTitle" />
            </awardTitle>
          </xsl:if>
        </fundingReference>
      </xsl:for-each>
    </fundingReferences>
  </xsl:template>

  <xsl:template match="re:language">
    <language>
      <xsl:choose>
        <xsl:when test=". = 'aar'">
          aa
        </xsl:when>
        <xsl:when test=". = 'abk'">
          ab
        </xsl:when>
        <xsl:when test=". = 'ave'">
          ae
        </xsl:when>
        <xsl:when test=". = 'afr'">
          af
        </xsl:when>
        <xsl:when test=". = 'aka'">
          ak
        </xsl:when>
        <xsl:when test=". = 'amh'">
          am
        </xsl:when>
        <xsl:when test=". = 'arg'">
          an
        </xsl:when>
        <xsl:when test=". = 'ara'">
          ar
        </xsl:when>
        <xsl:when test=". = 'asm'">
          as
        </xsl:when>
        <xsl:when test=". = 'ava'">
          av
        </xsl:when>
        <xsl:when test=". = 'aym'">
          ay
        </xsl:when>
        <xsl:when test=". = 'aze'">
          az
        </xsl:when>
        <xsl:when test=". = 'bak'">
          ba
        </xsl:when>
        <xsl:when test=". = 'bel'">
          be
        </xsl:when>
        <xsl:when test=". = 'bul'">
          bg
        </xsl:when>
        <xsl:when test=". = 'bis'">
          bi
        </xsl:when>
        <xsl:when test=". = 'bam'">
          bm
        </xsl:when>
        <xsl:when test=". = 'ben'">
          bn
        </xsl:when>
        <xsl:when test=". = 'bod'">
          bo
        </xsl:when>
        <xsl:when test=". = 'bre'">
          br
        </xsl:when>
        <xsl:when test=". = 'bos'">
          bs
        </xsl:when>
        <xsl:when test=". = 'cat'">
          ca
        </xsl:when>
        <xsl:when test=". = 'che'">
          ce
        </xsl:when>
        <xsl:when test=". = 'cha'">
          ch
        </xsl:when>
        <xsl:when test=". = 'cos'">
          co
        </xsl:when>
        <xsl:when test=". = 'cre'">
          cr
        </xsl:when>
        <xsl:when test=". = 'ces'">
          cs
        </xsl:when>
        <xsl:when test=". = 'chu'">
          cu
        </xsl:when>
        <xsl:when test=". = 'chv'">
          cv
        </xsl:when>
        <xsl:when test=". = 'cym'">
          cy
        </xsl:when>
        <xsl:when test=". = 'dan'">
          da
        </xsl:when>
        <xsl:when test=". = 'deu'">
          de
        </xsl:when>
        <xsl:when test=". = 'div'">
          dv
        </xsl:when>
        <xsl:when test=". = 'dzo'">
          dz
        </xsl:when>
        <xsl:when test=". = 'ewe'">
          ee
        </xsl:when>
        <xsl:when test=". = 'ell'">
          el
        </xsl:when>
        <xsl:when test=". = 'eng'">
          en
        </xsl:when>
        <xsl:when test=". = 'epo'">
          eo
        </xsl:when>
        <xsl:when test=". = 'spa'">
          es
        </xsl:when>
        <xsl:when test=". = 'est'">
          et
        </xsl:when>
        <xsl:when test=". = 'eus'">
          eu
        </xsl:when>
        <xsl:when test=". = 'fas'">
          fa
        </xsl:when>
        <xsl:when test=". = 'ful'">
          ff
        </xsl:when>
        <xsl:when test=". = 'fin'">
          fi
        </xsl:when>
        <xsl:when test=". = 'fij'">
          fj
        </xsl:when>
        <xsl:when test=". = 'fao'">
          fo
        </xsl:when>
        <xsl:when test=". = 'fra'">
          fr
        </xsl:when>
        <xsl:when test=". = 'fry'">
          fy
        </xsl:when>
        <xsl:when test=". = 'gle'">
          ga
        </xsl:when>
        <xsl:when test=". = 'gla'">
          gd
        </xsl:when>
        <xsl:when test=". = 'glg'">
          gl
        </xsl:when>
        <xsl:when test=". = 'grn'">
          gn
        </xsl:when>
        <xsl:when test=". = 'guj'">
          gu
        </xsl:when>
        <xsl:when test=". = 'glv'">
          gv
        </xsl:when>
        <xsl:when test=". = 'hau'">
          ha
        </xsl:when>
        <xsl:when test=". = 'heb'">
          he
        </xsl:when>
        <xsl:when test=". = 'hin'">
          hi
        </xsl:when>
        <xsl:when test=". = 'hmo'">
          ho
        </xsl:when>
        <xsl:when test=". = 'hrv'">
          hr
        </xsl:when>
        <xsl:when test=". = 'hat'">
          ht
        </xsl:when>
        <xsl:when test=". = 'hun'">
          hu
        </xsl:when>
        <xsl:when test=". = 'hye'">
          hy
        </xsl:when>
        <xsl:when test=". = 'her'">
          hz
        </xsl:when>
        <xsl:when test=". = 'ina'">
          ia
        </xsl:when>
        <xsl:when test=". = 'ind'">
          id
        </xsl:when>
        <xsl:when test=". = 'ile'">
          ie
        </xsl:when>
        <xsl:when test=". = 'ibo'">
          ig
        </xsl:when>
        <xsl:when test=". = 'iii'">
          ii
        </xsl:when>
        <xsl:when test=". = 'ipk'">
          ik
        </xsl:when>
        <xsl:when test=". = 'ido'">
          io
        </xsl:when>
        <xsl:when test=". = 'isl'">
          is
        </xsl:when>
        <xsl:when test=". = 'ita'">
          it
        </xsl:when>
        <xsl:when test=". = 'iku'">
          iu
        </xsl:when>
        <xsl:when test=". = 'jpn'">
          ja
        </xsl:when>
        <xsl:when test=". = 'jav'">
          jv
        </xsl:when>
        <xsl:when test=". = 'kat'">
          ka
        </xsl:when>
        <xsl:when test=". = 'kon'">
          kg
        </xsl:when>
        <xsl:when test=". = 'kik'">
          ki
        </xsl:when>
        <xsl:when test=". = 'kua'">
          kj
        </xsl:when>
        <xsl:when test=". = 'kaz'">
          kk
        </xsl:when>
        <xsl:when test=". = 'kal'">
          kl
        </xsl:when>
        <xsl:when test=". = 'khm'">
          km
        </xsl:when>
        <xsl:when test=". = 'kan'">
          kn
        </xsl:when>
        <xsl:when test=". = 'kor'">
          ko
        </xsl:when>
        <xsl:when test=". = 'kau'">
          kr
        </xsl:when>
        <xsl:when test=". = 'kas'">
          ks
        </xsl:when>
        <xsl:when test=". = 'kur'">
          ku
        </xsl:when>
        <xsl:when test=". = 'kom'">
          kv
        </xsl:when>
        <xsl:when test=". = 'cor'">
          kw
        </xsl:when>
        <xsl:when test=". = 'kir'">
          ky
        </xsl:when>
        <xsl:when test=". = 'lat'">
          la
        </xsl:when>
        <xsl:when test=". = 'ltz'">
          lb
        </xsl:when>
        <xsl:when test=". = 'lug'">
          lg
        </xsl:when>
        <xsl:when test=". = 'lim'">
          li
        </xsl:when>
        <xsl:when test=". = 'lin'">
          ln
        </xsl:when>
        <xsl:when test=". = 'lao'">
          lo
        </xsl:when>
        <xsl:when test=". = 'lit'">
          lt
        </xsl:when>
        <xsl:when test=". = 'lub'">
          lu
        </xsl:when>
        <xsl:when test=". = 'lav'">
          lv
        </xsl:when>
        <xsl:when test=". = 'mlg'">
          mg
        </xsl:when>
        <xsl:when test=". = 'mah'">
          mh
        </xsl:when>
        <xsl:when test=". = 'mri'">
          mi
        </xsl:when>
        <xsl:when test=". = 'mkd'">
          mk
        </xsl:when>
        <xsl:when test=". = 'mal'">
          ml
        </xsl:when>
        <xsl:when test=". = 'mon'">
          mn
        </xsl:when>
        <xsl:when test=". = 'mar'">
          mr
        </xsl:when>
        <xsl:when test=". = 'msa'">
          ms
        </xsl:when>
        <xsl:when test=". = 'mlt'">
          mt
        </xsl:when>
        <xsl:when test=". = 'mya'">
          my
        </xsl:when>
        <xsl:when test=". = 'nau'">
          na
        </xsl:when>
        <xsl:when test=". = 'nob'">
          nb
        </xsl:when>
        <xsl:when test=". = 'nde'">
          nd
        </xsl:when>
        <xsl:when test=". = 'nep'">
          ne
        </xsl:when>
        <xsl:when test=". = 'ndo'">
          ng
        </xsl:when>
        <xsl:when test=". = 'nld'">
          nl
        </xsl:when>
        <xsl:when test=". = 'nno'">
          nn
        </xsl:when>
        <xsl:when test=". = 'nor'">
          no
        </xsl:when>
        <xsl:when test=". = 'nbl'">
          nr
        </xsl:when>
        <xsl:when test=". = 'nav'">
          nv
        </xsl:when>
        <xsl:when test=". = 'nya'">
          ny
        </xsl:when>
        <xsl:when test=". = 'oci'">
          oc
        </xsl:when>
        <xsl:when test=". = 'oji'">
          oj
        </xsl:when>
        <xsl:when test=". = 'orm'">
          om
        </xsl:when>
        <xsl:when test=". = 'ori'">
          or
        </xsl:when>
        <xsl:when test=". = 'oss'">
          os
        </xsl:when>
        <xsl:when test=". = 'pan'">
          pa
        </xsl:when>
        <xsl:when test=". = 'pli'">
          pi
        </xsl:when>
        <xsl:when test=". = 'pol'">
          pl
        </xsl:when>
        <xsl:when test=". = 'pus'">
          ps
        </xsl:when>
        <xsl:when test=". = 'por'">
          pt
        </xsl:when>
        <xsl:when test=". = 'que'">
          qu
        </xsl:when>
        <xsl:when test=". = 'roh'">
          rm
        </xsl:when>
        <xsl:when test=". = 'run'">
          rn
        </xsl:when>
        <xsl:when test=". = 'ron'">
          ro
        </xsl:when>
        <xsl:when test=". = 'rus'">
          ru
        </xsl:when>
        <xsl:when test=". = 'kin'">
          rw
        </xsl:when>
        <xsl:when test=". = 'san'">
          sa
        </xsl:when>
        <xsl:when test=". = 'srd'">
          sc
        </xsl:when>
        <xsl:when test=". = 'snd'">
          sd
        </xsl:when>
        <xsl:when test=". = 'sme'">
          se
        </xsl:when>
        <xsl:when test=". = 'sag'">
          sg
        </xsl:when>
        <xsl:when test=". = 'sin'">
          si
        </xsl:when>
        <xsl:when test=". = 'slk'">
          sk
        </xsl:when>
        <xsl:when test=". = 'slv'">
          sl
        </xsl:when>
        <xsl:when test=". = 'smo'">
          sm
        </xsl:when>
        <xsl:when test=". = 'sna'">
          sn
        </xsl:when>
        <xsl:when test=". = 'som'">
          so
        </xsl:when>
        <xsl:when test=". = 'sqi'">
          sq
        </xsl:when>
        <xsl:when test=". = 'srp'">
          sr
        </xsl:when>
        <xsl:when test=". = 'ssw'">
          ss
        </xsl:when>
        <xsl:when test=". = 'sot'">
          st
        </xsl:when>
        <xsl:when test=". = 'sun'">
          su
        </xsl:when>
        <xsl:when test=". = 'swe'">
          sv
        </xsl:when>
        <xsl:when test=". = 'swa'">
          sw
        </xsl:when>
        <xsl:when test=". = 'tam'">
          ta
        </xsl:when>
        <xsl:when test=". = 'tel'">
          te
        </xsl:when>
        <xsl:when test=". = 'tgk'">
          tg
        </xsl:when>
        <xsl:when test=". = 'tha'">
          th
        </xsl:when>
        <xsl:when test=". = 'tir'">
          ti
        </xsl:when>
        <xsl:when test=". = 'tuk'">
          tk
        </xsl:when>
        <xsl:when test=". = 'tgl'">
          tl
        </xsl:when>
        <xsl:when test=". = 'tsn'">
          tn
        </xsl:when>
        <xsl:when test=". = 'ton'">
          to
        </xsl:when>
        <xsl:when test=". = 'tur'">
          tr
        </xsl:when>
        <xsl:when test=". = 'tso'">
          ts
        </xsl:when>
        <xsl:when test=". = 'tat'">
          tt
        </xsl:when>
        <xsl:when test=". = 'twi'">
          tw
        </xsl:when>
        <xsl:when test=". = 'tah'">
          ty
        </xsl:when>
        <xsl:when test=". = 'uig'">
          ug
        </xsl:when>
        <xsl:when test=". = 'ukr'">
          uk
        </xsl:when>
        <xsl:when test=". = 'uzb'">
          uz
        </xsl:when>
        <xsl:when test=". = 'ven'">
          ve
        </xsl:when>
        <xsl:when test=". = 'vie'">
          vi
        </xsl:when>
        <xsl:when test=". = 'vol'">
          vo
        </xsl:when>
        <xsl:when test=". = 'wln'">
          wa
        </xsl:when>
        <xsl:when test=". = 'wol'">
          wo
        </xsl:when>
        <xsl:when test=". = 'xho'">
          xh
        </xsl:when>
        <xsl:when test=". = 'yid'">
          yi
        </xsl:when>
        <xsl:when test=". = 'yor'">
          yo
        </xsl:when>
        <xsl:when test=". = 'zha'">
          za
        </xsl:when>
        <xsl:when test=". = 'zho'">
          zh
        </xsl:when>
        <xsl:when test=". = 'zul'">
          zu
        </xsl:when>
      </xsl:choose>
    </language>
  </xsl:template>

  <xsl:template name="sizes">
    <sizes>
      <size>
        <xsl:value-of select="$datasetSize" />
      </size>
    </sizes>
  </xsl:template>

  <xsl:template name="formats">
    <formats>
      <format>application/x-tar</format>
    </formats>
  </xsl:template>

</xsl:stylesheet>