# Changelog

All notable changes to this project will be documented in this file.

## [1.7.0] – 2026-09-01

### Changed

* **Migrated the Elasticsearch client from 7.17 (RHLC) to 9.5.**\
  Verify existing indices/mappings for compatibility after upgrading.
* Reused a single Elasticsearch client instance instead of opening/closing one per call, improving throughput.
* Elasticsearch bulk requests are now used for `reindexAll`, improving reindexing performance.
* Excluded unused CVE-flagged transitive dependencies (`httpclient5`, Jackson 3.x) pulled in by `elasticsearch-java`.
* Improved performance of reading multiple items and their content from Cassandra.
* Bumped `io.netty` to a newer version due to CVEs.
* Bumped Jackson to a newer version due to CVEs.
* Added the SonarQube/SonarJ plugin and fixed BLOCKER and CRITICAL Sonar issues.
* Added JaCoCo code coverage plugin, later bumped to a newer version.
* Extended OWASP dependency-check suppressions for unfixable transitive CVEs.
* Added missing JAXP XXE protections (external parameter entities, `ACCESS_EXTERNAL_DTD`/`ACCESS_EXTERNAL_STYLESHEET`) to the Saxon `TransformerFactory`.
* Compiled XSLT `Templates` are now cached per crosswalk instead of pooling raw `Transformer` objects, with cache eviction on crosswalk update/delete.

## [1.6.7] – 2026-08-05

### Changed

Upgrade netty libs due to high CVEs

## [1.6.6] – 2026-07-01

### Changed

Upgrade jackson libs due to high CVEs

## [1.6.5] – 2026-06-08

### Changed

Upgrade netty due to a high CVE

## [1.6.4] – 2026-04-08

### Changed

Upgrade netty due to a high CVE



## [1.6.3] – 2026-02-09

### Changed

Remove SOLR support.
From now on ElasticSearch is the primary search engine.

## [1.6.2] – 2026-01-13

### Changed

Bump SOLRJ to a higher version due to a critical CVE in zookeeper

## [1.6.1] – 2025-12-20

### Added

* **Status endpoint for Crosswalk processing**  
  `GET oai-backend/crosswalk/${crosswalkName}/status`

  Example response during processing:
```
  Crosswalk process STARTED on 2025-12-20T11:47:55.377029274Z
  Crosswalk: Radar2OAI_DC_v091001
  Progress: 85.21% (0:00:02 elapsed)
  ETA: 2025-12-20T11:47:57.377029274Z
```

### Changed

* Updated test framework to JUnit 5:\
The project now uses JUnit Jupiter, enabling modern testing features and improved IDE and build tool support.

* Improved Crosswalk processing performance:\
Crosswalk processing now leverages parallelStream to speed up execution, significantly reducing processing time for large datasets.

### Fixed

* Enforced single Crosswalk process at a time.  
  If a process is already running, subsequent requests now return a `409 Conflict` response.

## [1.6.0] – 2025-12-12

### Added

* Support for `resumptionToken` in the OAI-PMH `ListSets` verb, allowing large set lists to be retrieved in multiple requests.
  See: [https://www.openarchives.org/OAI/openarchivesprotocol.html#ListSets](https://www.openarchives.org/OAI/openarchivesprotocol.html#ListSets)
* Full support for hierarchical `setSpec` values as defined in the OAI-PMH specification.
  See: [https://www.openarchives.org/OAI/openarchivesprotocol.html#Set](https://www.openarchives.org/OAI/openarchivesprotocol.html#Set)

### Changed

* The service now runs on **Java 25 LTS**.
* The default pagination size for `ListSets` responses is **100**.
  This can be configured via the backend property:

  ```properties
  set.pagination.size=500
  ```

### Fixed

* Corrected the database schema for the `set` table.
  The primary key was previously incorrectly set to the `name` column and is now correctly set to the `spec` column.

### Breaking Changes

* Due to the corrected primary key in the `set` table, a **manual migration is required** after upgrading.

  After starting the new backend version, the system administrator must execute:

  ```bash
  curl -X POST http://localhost:8080/oai-backend/set/migrateOaiSets
  ```

  This endpoint migrates existing data to the new schema.

### Notes

* When creating hierarchical sets, **all parent sets must exist before creating a child set**.

  **Example:**
  To create `FIZ:ER:FD`, the following sets must be created in order:

    1. `FIZ`
    2. `FIZ:ER`
    3. `FIZ:ER:FD`

  If a required parent set is missing, the backend responds with **HTTP 400 (Bad Request)**.
