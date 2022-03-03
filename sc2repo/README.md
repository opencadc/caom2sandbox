# sc2repo

CAOM repository service for the CADC data engineering sandbox. 

## configuration
See the [cadc-tomcat](https://github.com/opencadc/docker-base/tree/master/cadc-tomcat) image
docs for expected deployment and common config requirements. The `sc2repo` war file can be renamed
at deployment time in order to support an alternate service name, including introducing
additional path elements (see war-rename.conf).

This service instance is expected to have a database backend to store the TAP metadata and which
also includes the caom2 tables.

Runtime configuration must be made available via the /config directory.

### catalina.properties (cadc-tomcat)
When running sc2repo.war in tomcat, parameters of the connection pool in META-INF/context.xml need to be configured in catalina.properties:

```
# database connection pools
sc2repo.test.maxActive={max connections for test admin pool}
sc2repo.test.username={username for test admin pool}
sc2repo.test.password={password for test admin pool}
sc2repo.test.url=jdbc:postgresql://{server}/{database}

sc2repo.sandbox.maxActive={max connections for sandbox admin pool}
sc2repo.sandbox.username={username for sandbox admin pool}
sc2repo.sandbox.password={password for sandbox admin pool}
sc2repo.sandbox.url=jdbc:postgresql://{server}/{database}
```

The `admin` account owns and manages (create, alter, drop) sandbox database objects and manages all the content (insert, update, delete).
In addition, the TAP service does not currently support a configurable schema name: it assumes a schema 
named `caom2` holds the content.


### LocalAuthority.properties
The LocalAuthority.properties file specifies which local service is authoritative for various site-wide functions. 
Documentation for the LocalAuthority.properties file can be found at [cadc-registry](https://github.com/opencadc/reg/tree/master/cadc-registry)


### sc2repo.properties
The sc2repo.properties configures a collection with the desired proposal group, operator group and staff group.
file uses the following format:

```
<collection> = <datasource name> <database> <schema> <obs table> <read-only group> <read-write group> <SQL generator class> basePublisherID=<ivo uri> [<key1=value1 key2=value2 ...>]
```

Each entry in the properties file configures a collection. Except for basePublisherID (mandatory), key=value pairs are optional. The computeMetadata option enables computation and persistence of <<computed>> metadata (generally, Plane metadata aggregated from the artifacts). The computeMetadataValidation options enables extra validation by performing the metadata computations, but the values are not persisted.

Documentation for the sc2repo.properties (CaomRepoConfig.properties) can be found at
[caom2-repo-server](https://github.com/opencadc/caom2db/tree/master/caom2-repo-server)


### database tables
sc2repo requires a PostgreSQL database with citext and pg_sphere extensions with a `caom2` schema. The `caom2` tables are created by calling the /sc2repo/availability endpoint, which checks/creates/upgrades the caom2 database content.


## building it
```
gradle clean build
docker build -t sc2repo -f Dockerfile .
```

## checking it
```
docker run --rm -it sc2repo:latest /bin/bash
```

## running it
```
docker run --rm --user tomcat:tomcat --volume=/path/to/external/config:/config:ro --name sc2repo sc2repo:latest
```