# sc2links

CAOM DataLink service for the Multiple Archive Query

## This code is OBSOLETE
The supported replacement is [bifrost](https://github.com/opencadc/caom2service/tree/master/bifrost)

A docker image available: `images.opencadc.org/caom2/bifrost` (use harbor API to check for version tags)

---

## configuration
See the [cadc-tomcat](https://github.com/opencadc/docker-base/tree/master/cadc-tomcat) image
docs for expected deployment and common config requirements. The `sc2links` war file can be renamed
at deployment time in order to support an alternate service name, including introducing
additional path elements (see war-rename.conf).

This service instance is expected to have a database backend to store the TAP metadata and which
also includes the caom2 tables.

Runtime configuration must be made available via the `/config` directory.

### catalina.properties
```
# database connection pools
   sc2links.uws.maxActive={max connections for jobs pool}
   sc2links.uws.username={database username for jobs pool}
   sc2links.uws.password={database password for jobs pool}
   sc2links.uws.url=jdbc:postgresql://{server}/{database}
```

The `uws` pool manages (create, alter, drop) uws tables and manages the uws content
(creates and modifies jobs in the uws schema when jobs are created and executed by users.

### LocalAuthority.properties
The LocalAuthority.properties file specifies which local service is authoritative for various site-wide functions.
Documentation for the LocalAuthority.properties file can be found at [cadc-registry](https://github.com/opencadc/reg/tree/master/cadc-registry)

## building it
```
gradle clean build
docker build -t sc2links -f Dockerfile .
```

## checking it
```
docker run --rm -it sc2links:latest /bin/bash
```

## running it
```
docker run --rm --user tomcat:tomcat --volume=/path/to/external/config:/config:ro --name sc2links sc2links:latest
```
