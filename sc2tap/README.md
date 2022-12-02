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
(creates and modifies jobs in the uws schema when jobs are created and executed by users).

The `tapadm` pool manages (create, alter, drop) tap_schema tables and manages the tap_schema content
for `tap_schema`, `caom2`, and `ivoa` schemas.

The `tapuser` pool is used to run TAP queries, including creating tables in the tap_upload schema. 

All three pools must have the same JDBC URL (e.g. use the same database) with PostgreSQL.

In addition, the TAP service does not currently support a configurable schema name: it assumes a schema 
named `caom2` holds the content.

### sc2tap.properties
This config file is no longer used.

### cadc-tap-tmp.properties
Temporary storage of uploads and async results are now handled by the 
[cadc-tap-tmp](https://github.com/opencadc/tap/tree/master/cadc-tap-tmp) library. This
library should be configured to use local mounted storage:
```
org.opencadc.tap.tmp.TempStorageManager.baseURL = https://{server name}/{service path}/results
org.opencadc.tap.tmp.TempStorageManager.baseStorageDir = {local directory}
```

Example:
```
org.opencadc.tap.tmp.TempStorageManager.baseURL = https://example.net/sc2tap/results
org.opencadc.tap.tmp.TempStorageManager.baseStorageDir = /var/tmp/sc2tap

```
works in the simple case because `/var/tmp` exists in the container and is writable by all. If deploying
multiple instances of `sc2tap` behind a balancer, a shared filesystem used by all containers is needed so
that a result file written by once instance can be served by another instance.

Note: `sc2tap` is currently hard-coded to use the TempStorageManager implementation because a plugin
config file used by the cadc-tap-server library has to be inside the war file. Once the library catalina
find and use the plugin config from the `/config` directory then it will be possible to chose the 
HttpStorageManager implementation.

### LocalAuthority.properties
The LocalAuthority.properties file specifies which local service is authoritative for various site-wide functions.
Documentation for the LocalAuthority.properties file can be found at [cadc-registry](https://github.com/opencadc/reg/tree/master/cadc-registry)

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
