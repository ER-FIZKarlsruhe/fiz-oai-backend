# FIZ OAI Backend

This repository provides the backend service for the FIZ-OAI system, leveraging Cassandra for data persistence and Elasticsearch for search capabilities.

## Prerequisites

- Java JDK 21 or higher
- Tomcat 11
- Apache Maven
- Cassandra 4.1
- Elasticsearch 7

## Building the Project
This project uses Maven for build management. Ensure Maven is installed and properly configured.

To build the project, run:

```bash
mvn clean package -U -Djavax.xml.accessExternalDTD=all -Djavax.xml.accessExternalSchema=all
```

## Configuration
The project configuration is managed through a `properties` file named `fiz-oai-backend.properties`, stored in the Tomcat conf folder. 
An example configuration with essential settings is provided below:

```properties
cassandra.keyspace=fizoaibackend1
cassandra.nodes=cassandra-oai:9042
cassandra.username=cassandra
cassandra.password=CHANGEME
cassandra.replication.factor={ 'class' : 'SimpleStrategy', 'replication_factor' : 1 }
cassandra.datacenter=datacenter1

elasticsearch.host=elasticsearch-oai
elasticsearch.port=9200

class.impl.search=de.fiz.oai.backend.service.impl.EsSearchServiceImpl

# Decide if deleted records persist in the database.
# Possible values: persistent, transient, no
# See https://www.openarchives.org/OAI/openarchivesprotocol.html#DeletedRecords
deletedRecord=persistent
```

### Key Configuration Parameters

- **Cassandra Settings**:
  - `cassandra.keyspace`: Defines the keyspace name.
  - `cassandra.nodes`: Specifies the Cassandra node addresses.
  - `cassandra.username` and `cassandra.password`: Authentication details.
  - `cassandra.replication.factor`: Cassandra replication strategy configuration.
  - `cassandra.datacenter`: Datacenter name for Cassandra.

- **Elasticsearch Settings**:
  - `elasticsearch.host`: Hostname or IP address of the Elasticsearch instance.
  - `elasticsearch.port`: Port on which Elasticsearch is running.

- **Deleted Records Management**:
  - `deletedRecord`: Defines how deleted records are managed in the database. Options are:
    - `persistent`: Deleted records persist.
    - `transient`: Deleted records persist temporarily.
    - `no`: Deleted records are not persisted.

## Running the Application
Once built, copy the war file into the webapps folder of a Tomcat 11 server:
Make sure your Cassandra and Elasticsearch instances are running and accessible based on your configuration.

## Initialisation
TODO

## REST-API
TODO

## Contributing

Contributions are welcome! Please open an issue or submit a pull request.

## License

See the `LICENSE` file in the repository for license information.

