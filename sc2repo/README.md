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
The LocalAuthority.properties file specifies which local service is authoritative for various site-wide functions. The keys are standardID values for the functions and the values are resourceID values for the service that implements that standard feature.

Example:
```
ivo://ivoa.net/std/GMS#search-0.1 = ivo://cadc.nrc.ca/gms           
ivo://ivoa.net/std/UMS#users-0.1 = ivo://cadc.nrc.ca/gms    
ivo://ivoa.net/std/UMS#login-0.1 = ivo://cadc.nrc.ca/gms           

ivo://ivoa.net/std/CDP#delegate-1.0 = ivo://cadc.nrc.ca/cred
ivo://ivoa.net/std/CDP#proxy-1.0 = ivo://cadc.nrc.ca/cred
```

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
sc2repo requires a PostgreSQL database with citext and pg_sphere extensions, and the `caom2` schema and tables. The `caom2` schema must first be created. The `caom2` tables are created using psql: the PostgreSQL client. 

The psql command to create a database table from a table definition is:
`psql -d <database> -h <host> -U <username> -W <prompt for password> -p <port> -f <filename>`

The `caom2` table definitions are in
[caom2 table definitions](https://github.com/opencadc/caom2db/tree/master/caom2persistence/src/main/resources/postgresql).

The placeholder `<schema>` in the table definitions can be replaced with a schema name using `sed`. To replace `<schema>` with `caom2` use `sed -i 's/<schema>/caom2/g' caom2.*.sql`

The caom2 tables are created in the following order:
```
caom2.ModelVersion.sql
caom2.Observation.sql
caom2.Plane.sql
caom2.Artifact.sql
caom2.Part.sql
caom2.Chunk.sql
caom2.HarvestState.sql
caom2.HarvestSkip.sql
caom2.HarvestSkipURI.sql
caom2.deleted.sql
caom2.extra_indices.sql
caom2.ObsCore.sql
caom2.ObsCore-x.sql
caom2.SIAv1.sql
caom2.permissions.sql
```

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