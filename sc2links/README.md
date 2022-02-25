# sc2links

CAOM DataLink service for the Multiple Archive Query

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
The LocalAuthority.properties file specifies which local service is authoritative for various site-wide functions. The keys
are standardID values for the functions and the values are resourceID values for the service that implements that standard
feature.

Example:
```
ivo://ivoa.net/std/GMS#search-0.1 = ivo://cadc.nrc.ca/gms
ivo://ivoa.net/std/UMS#users-0.1 = ivo://cadc.nrc.ca/gms
ivo://ivoa.net/std/UMS#login-0.1 = ivo://cadc.nrc.ca/gms

ivo://ivoa.net/std/CDP#delegate-1.0 = ivo://cadc.nrc.ca/cred
ivo://ivoa.net/std/CDP#proxy-1.0 = ivo://cadc.nrc.ca/cred
```

## building it
```
gradle clean build
docker build -t sc2meta -f Dockerfile .
```

## checking it
```
docker run --rm -it sc2links:latest /bin/bash
```

## running it
```
docker run --rm --user tomcat:tomcat --volume=/path/to/external/config:/config:ro --name sc2links sc2links:latest
```
