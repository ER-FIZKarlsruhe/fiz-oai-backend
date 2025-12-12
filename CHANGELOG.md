# Changelog

## [1.6.0] – 2025-12-12

### Fix wrong database schema for Set table
In previous versions the set table has the "name" column as primary key set.
This was wrong and has been changed to the "spec" column.

After starting the new backend version, the system administrator has to call the following endpoint:
oai-backend/set/migrateOaiSets

It will migrate the sets table to the new schema.

### Java 25 LTS
The service is running now with Java 25 LTS

### Set resumption token
As defined in the OAI-PMH specification, the ListSets verb supports resumptionToken as an parameter.
Which gives the harvester the possibility to call huge sets lists in multiple steps

https://www.openarchives.org/OAI/openarchivesprotocol.html#ListSets

By default the set pagination has a value of 100.
You can this in the backend property to another value:
set.pagination.size=500

### Set hierarchy
As defined in the OAI-PMH specification, sets can be defined using hierarchies
https://www.openarchives.org/OAI/openarchivesprotocol.html#Set

The OAI-Backend now fully supports setSpec with hierarchies.

Be aware, when creating hierarchies, all parent nodes have to be created before a leaf node!
E.g if you want to create the setSpec "FIZ:ER:FD"
You have to create first "FIZ", than "FIZ:ER" before you can create "FIZ:ER:FD".
If one of the parent nodes is missing a 400 response will indicate this.