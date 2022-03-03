# sc2tap

CAOM TAP service for the Multiple Archive Query (MAQ) 

This service allows queries to CAOM metadata using
IVOA <a href="http://www.ivoa.net/documents/TAP/20190927/">TAP-1.1</a> web service API.

## configuration
See the [cadc-tomcat](https://github.com/opencadc/docker-base/tree/master/cadc-tomcat) image
docs for expected deployment and common config requirements. The `sc2tap` war file can be renamed
at deployment time in order to support an alternate service name, including introducing
additional path elements (see war-rename.conf).

This service instance is expected to have a database backend to store the TAP metadata and which
also includes the caom2 tables.

Runtime configuration must be made available via the `/config` directory.

### catalina.properties
```
# database connection pools
sc2tap.uws.maxActive={max connections for jobs pool}
sc2tap.uws.username={database username for jobs pool}
sc2tap.uws.password={database password for jobs pool}
sc2tap.uws.url=jdbc:postgresql://{server}/{database}

sc2tap.tapadm.maxActive={max connections for jobs pool}
sc2tap.tapadm.username={database username for jobs pool}
sc2tap.tapadm.password={database password for jobs pool}
sc2tap.tapadm.url=jdbc:postgresql://{server}/{database}

sc2tap.tapuser.maxActive={max connections for jobs pool}
sc2tap.tapuser.username={database username for jobs pool}
sc2tap.tapuser.password={database password for jobs pool}
sc2tap.tapuser.url=jdbc:postgresql://{server}/{database}
```

The `uws` pool manages (create, alter, drop) uws tables and manages the uws content 
(creates and modifies jobs in the uws schema when jobs are created and executed by users.

The `tapadm` pool manages (create, alter, drop) tap_schema tables and manages the tap_schema content.

The `tapuser` pool is used to run TAP queries, including creating tables in the tap_upload schema. 

All three pools must have the same JDBC URL (e.g. use the same database) with PostgreSQL.

In addition, the TAP service does not currently support a configurable schema name: it assumes a schema 
named `caom2` holds the content.

### LocalAuthority.properties
The LocalAuthority.properties file specifies which local service is authoritative for various site-wide functions.
Documentation for the LocalAuthority.properties file can be found at [cadc-registry](https://github.com/opencadc/reg/tree/master/cadc-registry)

### database tables
sc2tap requires a PostgreSQL database with citext and pg_sphere extensions, the `caom2`, `tap_schema`, `tap_upload`, and `uws` schemas and tables.
The schemas must be created first. The `caom2` tables are created by calling the /sc2repo/availability endpoint.
The `tap_schema`, `tap_upload`, and `uws` tables can be created using psql commands.  The tables are created using psql: the PostgreSQL client.

The psql command to create a database table from a table definition is:
`psql -d <database> -h <host> -U <username> -W <prompt for password> -p <port> -f <filename>`

The placeholder `<schema>` in the table definitions can be replaced with a schema name using `sed`. To replace `<schema>` with `uws` use `sed -i 's/<schema>/uws/g' uws.*.sql`

#### uws tables

`uws` table definitions are in:
[cadc-uws-server](https://github.com/opencadc/uws/tree/master/cadc-uws-server/src/main/resources/postgresql)

Table creation order:
```
uws.ModelVersion.sql
uws.Job.sql
uws.JobDetail.sql
uws.JobAvailability.sql
uws.permissions.sql
```

#### tap_schema tables

`tap_schema` table definitions are in:
[cadc-tap-schema](https://github.com/opencadc/tap/tree/master/cadc-tap-schema/src/main/resources/postgresql)

Table creation order:
```
tap_schema.ModelVersion.sql
tap_schema.KeyValue.sql
tap_schema11.sql
tap_schema_self11.sql
tap_schema.permissions.sql
tap_schema.upgrade-1.2.0.sql
```

#### caom2 tap_schema content

caom2 tap_schema content definitions are in:
[caom2-tap-server](https://github.com/opencadc/caom2service/tree/master/caom2-tap-server/src/main/resources/sql)

```
caom2.tap_schema_content11.sql
ivoa.tap_schema_content11.sql
```

#### tap_upload table
`tap_upload` tables are dynamically created in the `tap_upload` schema.


## building it
```
gradle clean build
docker build -t sc2tap -f Dockerfile .
```

## checking it
```
docker run --rm -it sc2tap:latest /bin/bash
```

## running it
```
docker run --rm --user tomcat:tomcat --volume=/path/to/external/config:/config:ro --name sc2tap sc2tap:latest
```
